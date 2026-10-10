import type { HardhatEthersSigner } from "@nomicfoundation/hardhat-ethers/types";
import { expect } from "chai";
import { network } from "hardhat";
import { keccak256, type TypedDataDomain, type TypedDataField, encodeBytes32String } from "ethers";
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

  const hash = ethers.TypedDataEncoder.hash(domain, types, value)
  const signature = await owner.signTypedData(domain, types, value)
  return {hash, signature, value, domain}
}

async function approveAndPay(
  token: MockERC20, 
  buyer: HardhatEthersSigner,
  gateway: PaymentRail,
  domain: Record<string, any>, 
  value: Record<string, any>,
  hash: string,
  signature: string
) {
  await token.connect(buyer).approve(await gateway.getAddress(), TRANSFER_AMOUNT)
  await gateway.connect(buyer).pay(
    value,
    domain,
    signature, 
    hash)
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

  it("Initially, owner of PaymentRail is also the server")

  it("Only owner can set the trusted server address", async function() {
    const [owner, nonOwner] = await ethers.getSigners();
    const gatewayFactory = await ethers.getContractFactory("PaymentRail");
    const gateway = await gatewayFactory.connect(owner).deploy();
    
    expect(await gateway.connect(owner).setServerAddr(TRUSTED_SERVER_ADDR))
    expect(await gateway.connect(nonOwner).setServerAddr(UNTRUSTED_SERVER_ADDR)).to.be.revertedWithCustomError(gateway, "j")
  })
  
  it("Transfer exact amount from buyer to merchant", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {hash, signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)
    expect(await token.balanceOf(merchant.address)).equals(0);
    expect(await token.balanceOf(buyer.address)).equals(TRANSFER_AMOUNT);

    await approveAndPay(token, buyer, gateway, domain, value, hash, signature)

    expect(await token.balanceOf(merchant.address)).equals(TRANSFER_AMOUNT);
    expect(await token.balanceOf(buyer.address)).equals(0);
  })

  it("Contract holds no tokens after payment (non-custodial)", async function() {
    const {token, owner, merchant, buyer, gateway} = await deploySystem();
    const {hash, signature, value, domain} = await serverCreateInvoice(INVOICE_ID, TRANSFER_AMOUNT, owner, token, merchant, gateway)

    expect(await token.balanceOf(await gateway.getAddress())).equals(0)
    await approveAndPay(token, buyer, gateway, domain, value, hash, signature)
    expect(await token.balanceOf(await gateway.getAddress())).equals(0)
  })
  
  it("Same buyer can pay two different invoices")
  it("Succesful transfer emits PaymentRecieved event with correct args")
  it("An expired signature cannot be used")
  it("User cannot pay twice on the same invoice")
  it("Payment at exactly the deadline succeeds")
  it("Insufficient allowance reverts")
  it("Insufficient balance reverts")
  it("Cannot set signer to zero address")
  it("Failed payment does not mark invoice as paid")
  it("Tamper with invoiceId invalidates signature")
  it("Tamper with tokenAddr invalidates signature")
  it("Tamper with amount invalidates signature")
  it("Tamper with merchantAddr invalidates signature")
  it("Tamper with deadline invalidates signature")
  it("Cannot validate a signature on a different blockchain network")

  it("Malformed signature reverts")
  it("Signature for another contract cannot be used")
  it("A valid signature signed by someone else cannot be used to pay")
});
