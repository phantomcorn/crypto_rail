# Getting started

Set your private key and rpc url using 
```
npx hardhat keystore set PYROCRYO_SEPOLIA_PRIVATE_KEY         
```
and 
```
npx hardhat keystore set PYROCRYO_SEPOLIA_RPC_URL        
```


# Deploying

Run
```
make deploy
```

We use hardhat scripts instead of ignition modules. The reason is because ignition doesn't fully support UUPS pattern. 

## Upgrading

To deploy an upgrade modify the variable:
```js
// /scripts/upgrade.ts
...
const UPGRADE_CONTRACT = "name of your upgraded contract"
...
```

An upgradable contract should also override the `version()` function for sanity check.

Once that's done, run 
```
make upgrade
```
A successful upgrade should print the version number you override.
# Upgrading contracts

An upgrading contracts should always follow the same interface as `/contracts/PaymentRail.sol`. Any variables declaration in the upgrading contracts should also have the same variable declaration as the founding implementation contract. NEVER reorder your variables. Treat the declaration block as a simple copy-paste. A way to keep this simpler is to use inheritance When inheriting, only declare NEW variables never the old one.

# FAQ

### In `PaymentRail`, I seperate owner and signer. Why not keep them the same?

Seperate security levels. Owner concerns with the upgrading of contracts whereas signer concerns with signing signatures. We need not mix the two up. Signer is more user-facing so chances of it being leaked is higher. Owner can swap out signer when this happens.

