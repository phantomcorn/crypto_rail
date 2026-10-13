import { buildModule } from "@nomicfoundation/hardhat-ignition/modules";

export default buildModule("PaymentRail", (m) => {
    const owner = m.getParameter("owner")
    const server = m.getParameter("server")

    const token = m.contract("PaymentRail", []);
    m.call(token, "setServerAddr", [server])

    return { token };
});