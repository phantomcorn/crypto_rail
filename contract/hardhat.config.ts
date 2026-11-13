import hardhatToolboxMochaEthersPlugin from "@nomicfoundation/hardhat-toolbox-mocha-ethers";
import { configVariable, defineConfig } from "hardhat/config";
import hardhatUpgrades from '@openzeppelin/hardhat-upgrades';
export default defineConfig({
  plugins: [hardhatToolboxMochaEthersPlugin, hardhatUpgrades],
  solidity: {
    profiles: {
      default: {
        version: "0.8.34",
      },
      production: {
        version: "0.8.34",
        settings: {
          optimizer: {
            enabled: true,
            runs: 200,
          },
        },
      },
    },
    settings: {
      outputSelection: {
        '*': {
          '*': ['storageLayout'],
        },
      },
    },
  },
  networks: {
    hardhatMainnet: {
      type: "edr-simulated",
      chainType: "l1",
    },
    hardhatOp: {
      type: "edr-simulated",
      chainType: "op",
    },
    sepolia: {
      type: "http",
      chainType: "l1",
      url: configVariable("PYROCRYO_SEPOLIA_RPC_URL"),
      accounts: [configVariable("PYROCRYO_SEPOLIA_PRIVATE_KEY")],
    },
  },
});