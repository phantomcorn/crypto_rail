import hre from "hardhat";
import { upgrades } from '@openzeppelin/hardhat-upgrades';
import params from "./parameters.js"

const connection = await hre.network.create();
const { ethers } = connection;
const upgradesApi = await upgrades(hre, connection);

const SIGNER_ADDRESS = params.signer;

const gateway = await ethers.getContractFactory("PaymentRail");
const v1 = await upgradesApi.deployProxy(gateway, [SIGNER_ADDRESS], {kind: "uups"})
await v1.waitForDeployment()

const proxy = await v1.getAddress()
const impl = await upgradesApi.erc1967.getImplementationAddress(proxy);
console.log("Version " + await v1.version())
console.log({ proxy, impl, chainId: (await ethers.provider.getNetwork()).chainId})
