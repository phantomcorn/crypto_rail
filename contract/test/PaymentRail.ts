import type { HardhatEthersSigner } from "@nomicfoundation/hardhat-ethers/types";
import { expect } from "chai";
import { network } from "hardhat";
import { Wallet,  encodeBytes32String } from "ethers";
import type { PaymentRail } from "../types/ethers-contracts/PaymentRail.js";
import type { MockERC20 } from "../types/ethers-contracts/MockERC20.js";

const { ethers, networkHelpers } = await network.create();

const TRUSTED_SERVER_ADDR = "0x0123456789023456789001234567890234567890"
const UNTRUSTED_SERVER_ADDR = "0x9876543210987654321098765432109876543210"
const REAL_NETWORK = 1
const FAKE_NETWORK = 11155111
const TRANSFER_AMOUNT = 30;
const INVOICE_ID = "32"
const FIVE_MINUTES = 300
const TAMPERED_INVOICE_ID = "99"
const TAMPERED_TOKEN_ADDR = "0xf531B8F309be94191af87605cfbf600d71c2cfe0".toLowerCase()
const TAMPERED_AMOUNT = 99
const TAMPERD_MERCHANT_ADDR = Wallet.createRandom().address

async function deploySystem() {
  const [owner, buyer, merchant] = await ethers.getSigners()
  const token = await ethers.deployContract("MockERC20");
  await token.mint(buyer.address, TRANSFER_AMOUNT);
  const gateway = await ethers.deployContract("PaymentRail");
  await gateway.setServerAddr(owner.address);
  return {buyer, merchant, owner, token, gateway}
}

async function serverCreateInvoice( 
  invoiceId: string,
  amount: number,
  owner: HardhatEthersSigner,
  token: MockERC20, 
  merchant: HardhatEthersSigner, 
  gateway: PaymentRail,
) {

  const types = {
  /* From contracts/PaymentRail.sol
    struct Invoice {
        bytes32 id;
        address tokenAddr;
        uint amount;
        address merchantAddr;
        uint deadline;
    }
  */
    Invoice: [
      { name: "id", type: "bytes32"},
      { name: "tokenAddr", type: "address"},
      { name: "amount", type: "uint256"},
      { name: "merchantAddr", type: "address"},
      { name: "deadline", type: "uint256"},
    ]
  }

  const domain = {
    name: "app",
    version: "1",
    chainId: REAL_NETWORK,
    verifyingContract: await gateway.getAddress(),
  }
  const value = {
    id: encodeBytes32String(invoiceId),
    tokenAddr: await token.getAddress(),
    amount: amount,
    merchantAddr: await merchant.getAddress(),
    deadline: await networkHelpers.time.latest() + FIVE_MINUTES
  }
  const signature = await owner.signTypedData(domain, types, value)
  return {signature, value, domain, types}
}

async function approveAndPay(
  token: MockERC20, 
  buyer: HardhatEthersSigner,
  gateway: PaymentRail,
  domain: Record<string, any>, 
  value: Record<string, any>,
  signature: string
) {
  await token.connect(buyer).approve(await gateway.getAddress(), value.amount) //if current time is t then this gets mined at block t=n+1
  await gateway.connect(buyer).pay( //similarly, pay mined at t + 2
    value,
    domain,
    signature)
}

describe("PaymentRail", function () {
  it("Can construct", async function () {
    const gateway = await ethers.deployContract("PaymentRail");
  });

  it("Owner of PaymentRail is deployer", async function() {
    const [owner, nonOwner] = await ethers.getSigners();
    const gatewayFactory = await ethers.getContractFactory("PaymentRail");
    const gateway = await gatewayFactory.connect(owner).deploy();
    
    expect(await gateway.owner()).equals(owner.address);
    expect(await gateway.owner()).is.not.equals(nonOwner.address)
  })

  it("Only owner can set the trusted server address", async function() {
    const [owner, nonOwner] = await ethers.getSigners();
    const gatewayFactory = await ethers.getContractFactory("PaymentRail");
    const gateway = await gatewayFactory.connect(owner).deploy();
    expect(await gateway.owner()).equals(owner.address);
    expect(await gateway.connect(owner).setServerAddr(TRUSTED_SERVER_ADDR))
    await expect(gateway.connect(nonOwner).setServerAddr(UNTRUSTED_SERVER_ADDR)).to.be.revertedWithCustomError(gateway, "OwnableUnauthorizedAccount")
  })
  
  it("Transfer exact amount from buyer to merchant", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    expect(await token.balanceOf(merchant.address)).equals(0);
    expect(await token.balanceOf(buyer.address)).equals(TRANSFER_AMOUNT);

    await approveAndPay(token, buyer, gateway, domain, value, signature)

    expect(await token.balanceOf(merchant.address)).equals(TRANSFER_AMOUNT);
    expect(await token.balanceOf(buyer.address)).equals(0);
  })

  it("Contract holds no tokens after payment (non-custodial)", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)

    expect(await token.balanceOf(await gateway.getAddress())).equals(0)
    await approveAndPay(token, buyer, gateway, domain, value, signature)
    expect(await token.balanceOf(await gateway.getAddress())).equals(0)
  })
  
  it("Same buyer can pay two different invoices", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT / 2, owner, token, merchant, gateway)

    expect(await token.balanceOf(await merchant.getAddress())).equals(0)
    
    await approveAndPay(token, buyer, gateway, domain, value, signature)
    expect(await token.balanceOf(await merchant.getAddress())).equals(TRANSFER_AMOUNT / 2)
    
    const {signature: newSignature, value: newValue, domain: newDomain} = await serverCreateInvoice(INVOICE_ID + 1, TRANSFER_AMOUNT / 2, owner, token, merchant, gateway)
  
    await approveAndPay(token, buyer, gateway, newDomain, newValue, newSignature)
    expect(await token.balanceOf(await merchant.getAddress())).equals(TRANSFER_AMOUNT)
  })

  it("Succesful transfer emits PaymentReceived event with correct args", async function () {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    await token.connect(buyer).approve(await gateway.getAddress(), value.amount)
    await expect(await gateway
      .connect(buyer)
      .pay(
        value,
        domain,
        signature)
      ).to.emit(gateway, "PaymentReceived").withArgs(value.id)
  })

  it("An expired signature cannot be used", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    
    await networkHelpers.time.increase(value.deadline + 1) //expires signature
    await expect(approveAndPay(token, buyer, gateway, domain, value, signature)).to.revertedWith("Signature expired")
  })

  it("User cannot pay twice on the same invoice", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT/2, owner, token, merchant, gateway)

    expect(await token.balanceOf(await merchant.getAddress())).equals(0)
    await approveAndPay(token, buyer, gateway, domain, value, signature)
    expect(await token.balanceOf(await merchant.getAddress())).equals(TRANSFER_AMOUNT/2)
    await expect(approveAndPay(token, buyer, gateway, domain, value, signature)).to.be.revertedWith("Invoice already paid")
  })

  it("Payment at exactly the deadline succeeds", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    
    expect(await token.balanceOf(merchant.address)).equals(0);
    expect(await token.balanceOf(buyer.address)).equals(value.amount);

    await token.connect(buyer).approve(await gateway.getAddress(), value.amount) //if current time is t then this gets mined at block t=n+1
    await networkHelpers.time.setNextBlockTimestamp(value.deadline)
    await gateway.connect(buyer).pay( //block gets mined at t=value.deadline -> exactly at deadline
      value,
      domain,
      signature
    )
    expect(await networkHelpers.time.latest()).equals(value.deadline) //Current block e.g block that pay lives in is exactly at deadline

    expect(await token.balanceOf(merchant.address)).equals(value.amount);
    expect(await token.balanceOf(buyer.address)).equals(0);
  })

  it("Insufficient allowance reverts", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    
    await token.connect(buyer).approve(await gateway.getAddress(), value.amount - 1)
    await expect(gateway.connect(buyer).pay(
      value,
      domain,
      signature)).to.be.revertedWithCustomError(token, "ERC20InsufficientAllowance")
  })

  it("Insufficient balance reverts", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT + 1, owner, token, merchant, gateway)
    expect(await token.balanceOf(buyer.address)).equals(TRANSFER_AMOUNT)
    await token.connect(buyer).approve(await gateway.getAddress(), TRANSFER_AMOUNT + 1)
    await expect(gateway.connect(buyer).pay(
      value,
      domain,
      signature)).to.be.revertedWithCustomError(token,"ERC20InsufficientBalance")
    })

  it("Cannot set signer to zero address", async function() {
    const {gateway} = await deploySystem()
    await expect(gateway.setServerAddr(ethers.ZeroAddress)).to.revertedWith("Cannot set zero address")
  })

  it("Tamper with invoiceId invalidates signature", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    const tamperedValue = {...value, id: encodeBytes32String(TAMPERED_INVOICE_ID)}

    await expect(approveAndPay(token, buyer, gateway, domain, tamperedValue, signature)).to.be.revertedWith("Invalid signature")
  })
  
  it("Tamper with tokenAddr invalidates signature", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    const tamperedValue = {...value, tokenAddr: TAMPERED_TOKEN_ADDR}

    await expect(approveAndPay(token, buyer, gateway, domain, tamperedValue, signature)).to.be.revertedWith("Invalid signature")
  })
  it("Tamper with amount invalidates signature", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    const tamperedValue = {...value, amount: TAMPERED_AMOUNT}

    await expect(approveAndPay(token, buyer, gateway, domain, tamperedValue, signature)).to.be.revertedWith("Invalid signature")
  })
  it("Tamper with merchantAddr invalidates signature", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    const tamperedValue = {...value, merchantAddr: TAMPERD_MERCHANT_ADDR}

    await expect(approveAndPay(token, buyer, gateway, domain, tamperedValue, signature)).to.be.revertedWith("Invalid signature")
  })

  it("Tamper with deadline (increase) invalidates signature", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway) 
    //original deadline = t + 300
    //tampered deadline = t + 300 + 699 = t + 999
    const tamperedValue = {...value, deadline: value.deadline + 699} 
    await token.connect(buyer).approve(await gateway.getAddress(), value.amount) //t + 997
    await expect(gateway.connect(buyer).pay(tamperedValue, domain, signature)).to.be.revertedWith("Invalid signature") //t = TAMPERED_DEADLINE - 2
  })

  it("Tamper with deadline (decrease) will revert with expired signature", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    //original deadline = t + 300
    //tampered deadline = t + 300 - 301 = t - 1
    const tamperedValue = {...value, deadline: value.deadline - 301}

    expect(await networkHelpers.time.latest()).greaterThan(tamperedValue.deadline) //expired signature
    await expect(approveAndPay(token, buyer, gateway, domain, tamperedValue, signature)).to.be.revertedWith("Signature expired")
  })

  it("Cannot validate a signature on a different blockchain network", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    const tamperedDomain= {...domain, chainId: FAKE_NETWORK}
    await expect(approveAndPay(token, buyer, gateway, tamperedDomain, value, signature)).to.be.revertedWith("Invalid signature")
  })

  it("Malformed signature reverts", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    const malformedSig = `${signature}00`
    await expect(approveAndPay(token, buyer, gateway, domain, value, malformedSig)).to.be.revertedWith("Invalid signature length")
  })

  it("Valid signature from another invoice cannot be used to pay for another invoice", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const invoice1 = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    const invoice2 = await serverCreateInvoice(INVOICE_ID + 1, TRANSFER_AMOUNT, owner, token, merchant, gateway)

    await expect(approveAndPay(token, buyer, gateway, invoice1.domain, invoice1.value, invoice2.signature)).to.be.revertedWith("Invalid signature")
  })

  it("A valid signature signed by someone else cannot be used to pay", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {types, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    const signatureSignedBySomeoneElse = await buyer.signTypedData(domain, types, value)
    await expect(approveAndPay(token, buyer, gateway, domain, value, signatureSignedBySomeoneElse)).to.be.revertedWith("Invalid signature")
  })

});
