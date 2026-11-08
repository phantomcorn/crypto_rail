// SPDX-License-Identifier: UNLICENSED
pragma solidity^0.8.0;

struct Invoice {
        bytes32 id;
        address tokenAddr;
        uint amount;
        address merchantAddr;
        uint deadline;
}

struct EIP712Domain {
    string name;
    string version;
    uint256 chainId;
    address verifyingContract;
}

event PaymentReceived(bytes32 indexed invoiceId);