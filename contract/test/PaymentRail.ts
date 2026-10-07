import { expect } from "chai";
import { network } from "hardhat";

const { ethers } = await network.create();

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
