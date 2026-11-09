import hre from "hardhat";
import { upgrades } from '@openzeppelin/hardhat-upgrades';
import params from "./parameters.js"

const connection = await hre.network.create();
const { ethers } = connection;
const upgradesApi = await upgrades(hre, connection);

const PROXY_ADDRESS = params.contracts.proxy
const UPGRADE_CONTRACT = "PaymentRailV2"

const newImplFactory = await ethers.getContractFactory(UPGRADE_CONTRACT);
const before  = await upgradesApi.erc1967.getImplementationAddress(PROXY_ADDRESS);
const newImpl = (await upgradesApi.prepareUpgrade(PROXY_ADDRESS, newImplFactory, { kind: "uups" })) as string;

if (newImpl.toLowerCase() === before.toLowerCase()) {
  console.log("Already on this implementation, nothing to do:", before);
  process.exit(0);
}

const v2 = await ethers.getContractAt(UPGRADE_CONTRACT, PROXY_ADDRESS);
const tx = await v2.upgradeToAndCall(newImpl, "0x");
await tx.wait(2);

const impl = await upgradesApi.erc1967.getImplementationAddress(PROXY_ADDRESS);
console.log("Version " + await v2.version())
console.log({ proxy: PROXY_ADDRESS, impl, chainId: (await ethers.provider.getNetwork()).chainId})