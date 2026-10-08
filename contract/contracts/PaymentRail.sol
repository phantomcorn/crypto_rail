// SPDX-License-Identifier: UNLICENSED
pragma solidity^0.8.0;

import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";

interface IERC20 {
    function transferFrom(address from, address to, uint256 value) external returns (bool);
}

contract PaymentRail is Ownable {

    address public serverAddr;
    mapping(bytes32 => bool) private hasPaid;

    event PaymentRecieved(bytes32 invoiceId, uint amount, address merchantAddr);

    constructor() Ownable(msg.sender) {}

    function pay(bytes32 invoiceId, address tokenAddr, uint amount, address merchantAddr, uint deadline, bytes memory signature, bytes32 digest) external {
        require(!hasPaid[invoiceId], "Invoice already paid");
        require(deadline > block.timestamp, "Signature expired");

        (uint8 v, bytes32 r, bytes32 s) = extractVRS(signature);
        address signerAddr = ecrecover(digest, v,r,s);

        require(signerAddr != address(0), "Invalid signature");
        require(signerAddr == serverAddr, "Signer must be from real server address");

        IERC20(tokenAddr).transferFrom(msg.sender, merchantAddr, amount);
        emit PaymentRecieved(invoiceId, amount, merchantAddr);
    }

    function setServerAddr(address newServerAddr) external onlyOwner {
        serverAddr = newServerAddr;
    }

    function extractVRS(bytes32 signature) internal pure returns(uint8, bytes32, bytes32) {
        require(signature.length == 65, "Invalid signature length");

        uint8 v;
        bytes32 r;
        bytes32 s;
        assembly {
            /*
            First 32 bytes of a dynamic array store the length of the array.
            We skip the first 32 bytes to read the actual data.
            */

            // Load the next 32 bytes into r
            r := byte(0,mload(add(signature, 32)))

            // Load the next 32 bytes into s
            s := byte(0,mload(add(signature, 64)))

            // Load the final byte into v (mload reads 32 bytes, but we mask/cast it)
            v := mload(add(signature, 96))
        }

        return (uint8(v),bytes32(r),bytes32(s));
    }

}