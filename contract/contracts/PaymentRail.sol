// SPDX-License-Identifier: UNLICENSED
pragma solidity^0.8.0;

import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";

interface IERC20 {
    function transferFrom(address from, address to, uint256 value) external returns (bool);
}

contract PaymentRail is Ownable {

    address public serverAddr;
    mapping(bytes32 => bool) private hasPaid;

    event PaymentRecieved(bytes32 indexed invoiceId);

    bytes32 private constant EIP712_DOMAIN_TYPEHASH = keccak256(
        "EIP712Domain(string name,string version,uint256 chainId,address verifyingContract)"
    );

    bytes32 private constant MESSAGE_TYPEHASH = keccak256(
        "Invoice(bytes32 id,address tokenAddr,uint256 amount,address merchantAddr,uint256 deadline)"
    );
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

    constructor() Ownable(msg.sender) {}

    function pay(Invoice memory invoice, EIP712Domain memory domain, bytes memory signature) external {
        require(!hasPaid[invoice.id], "Invoice already paid");
        require(invoice.deadline >= block.timestamp, "Signature expired");

        verify(invoice, domain, signature);

        hasPaid[invoice.id] = true;
        IERC20(invoice.tokenAddr).transferFrom(msg.sender, invoice.merchantAddr, invoice.amount);
        emit PaymentRecieved(invoice.id);
    }

    function setServerAddr(address newServerAddr) external onlyOwner {
        require(newServerAddr != address(0), "Cannot set zero address");
        serverAddr = newServerAddr;
    }

    function verify(Invoice memory invoice, EIP712Domain memory domain, bytes memory signature) internal view {
        bytes32 domainSeparator = hashStructDomain(domain);
        bytes32 computedDigest = keccak256(abi.encodePacked("\x19\x01",domainSeparator,hashStructInvoice(invoice)));        
        require(signature.length == 65, "Invalid signature length");
        uint8 v;
        bytes32 r;
        bytes32 s;
        assembly {
            /*
            First 32 bytes of a dynamic array store the length of the array.
            We skip the first 32 bytes to read the actual data.
            */
            r := mload(add(signature, 32))
            s := mload(add(signature, 64))
            v := byte(0, mload(add(signature, 96)))
        }
        address signer = ecrecover(computedDigest, v,r,s);
        require(signer != address(0) && signer == serverAddr , "Invalid signature");
    }

    function hashStructInvoice(Invoice memory invoice) internal pure returns(bytes32) {
        return keccak256(
            abi.encode(
                MESSAGE_TYPEHASH,
                invoice.id,
                invoice.tokenAddr,
                invoice.amount,
                invoice.merchantAddr,
                invoice.deadline
            )
        );
    }

    function hashStructDomain(EIP712Domain memory domain) internal pure returns(bytes32) {
        return keccak256(
            abi.encode(
                EIP712_DOMAIN_TYPEHASH,
                keccak256(bytes(domain.name)),  //"The dynamic values bytes and string are encoded as a keccak256 hash of their contents". Source: https://eips.ethereum.org/EIPS/eip-712
                keccak256(bytes(domain.version)),
                domain.chainId,
                domain.verifyingContract
            )
        );
    }

}