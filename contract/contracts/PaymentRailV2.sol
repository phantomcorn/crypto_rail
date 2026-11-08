// SPDX-License-Identifier: UNLICENSED
pragma solidity^0.8.0;

import "./PaymentRail.sol";
import {Invoice, EIP712Domain, PaymentReceived} from "./Types.sol";

/// @custom:oz-upgrades-from contracts/PaymentRail.sol:PaymentRail
contract PaymentRailV2 is PaymentRail {
    
    function pay(Invoice memory invoice, EIP712Domain memory domain, bytes memory signature) override external {
        require(!hasPaid[invoice.id], "Invoice already paid");
        verify(invoice, domain, signature);
        require(invoice.deadline >= block.timestamp, "Signature expired");

        hasPaid[invoice.id] = true;
        IERC20(invoice.tokenAddr).transferFrom(msg.sender, invoice.merchantAddr, invoice.amount);
        emit PaymentReceived(invoice.id);
    }

} 