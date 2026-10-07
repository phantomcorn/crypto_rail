import type { HardhatEthersSigner } from "@nomicfoundation/hardhat-ethers/types";
import { expect } from "chai";
import { network } from "hardhat";
import { keccak256,encodeBytes32String  } from "ethers";

const { ethers } = await network.create();

const TRUSTED_SERVER_ADDR = "0x01234567890234567890"
const UNTRUSTED_SERVER_ADDR = "0x98765432109876543210"
const REAL_NETWORK = "1"
const FAKE_NETWORK = "11155111"
const TRANSFER_AMOUNT = 30;
const INVOICE_ID = "32"
const FIVE_MINUTES = 300

interface InvoiceDetails {
  invoiceId: string,
  tokenAddr: string,
  amount: number,
  deadline: number
}

interface DomainDetails {
  appName: string
  version: string
  contractAddr: string
  networkId: string
}



async function deploySystem() {
  const [owner, buyer, merchant] = await ethers.getSigners()
  const token = await ethers.deployContract("MockERC20");
  await token.mint(buyer.address, TRANSFER_AMOUNT);
  const gateway = await ethers.deployContract("PaymentRail");
  const validInvoice: InvoiceDetails = {
    invoiceId: INVOICE_ID,
    tokenAddr: await token.getAddress(),
    amount: 30,
    deadline: Date.now() + FIVE_MINUTES
  }
  const validDomain: DomainDetails = {
    appName: "Bean",
    version: "1",
    contractAddr: await gateway.getAddress(),
    networkId: REAL_NETWORK
  }
  return {buyer, merchant, owner, token, gateway, validInvoice, validDomain}
}

async function generateSignature(
  signer: HardhatEthersSigner, 
  invoiceDetails: InvoiceDetails,
  domainDetails: DomainDetails
) {
  const structHash = hashStruct(invoiceDetails)
  const domainSeperator = hashStructDomain(domainDetails)

  const signature = signer.signMessage(`\x19\x01${structHash}${domainSeperator}`)
  return signature
}
  

function hashStruct(data: InvoiceDetails) {
  const {invoiceId, tokenAddr, amount ,deadline} = data
  const typeHash = keccak256("pay(bytes32 invoiceId,address tokenAddr,uint amount,address merchantAddr,uint deadline,bytes32 signature)")
  const encodeData = encode(invoiceId) + encode(tokenAddr) + encode(`${amount}`) +encode(`${deadline}`)
  return keccak256(typeHash + encodeData)
}

function hashStructDomain(data: DomainDetails) {
  const {appName, version, contractAddr, networkId} = data
  const typeHash = keccak256("EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)")
  const encodeData = encode(appName) + encode(version) + encode(contractAddr) + encode(networkId)
  return keccak256(typeHash + encodeData)
}

function encode(data: string) {
  return encodeBytes32String(data)
}

describe("PaymentRail", function () {
  it("Can construct", async function () {});

  it("Owner of PaymentRail is deployer", async function() {})

  it("Only owner can set the trusted server address", async function() {})
  
  it("Transfer exact amount from buyer to merchant", async function() {})

  it("Contract holds no tokens after payment (non-custodial)")
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
