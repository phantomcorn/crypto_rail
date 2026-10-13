async function connectWallet() {
    // Check if MetaMask is installed
    if (typeof window.ethereum !== 'undefined') {
        try {
            // Trigger MetaMask login and account access request
            const accounts = await window.ethereum.request({ method: 'eth_requestAccounts' });

            // Get the connected wallet address
            const account = accounts[0];

            // Update UI
            document.getElementById('walletAddress').innerText = "Connected Account: " + account;
            document.getElementById('connectButton').innerText = "Connected";
        } catch (error) {
            console.error("User denied account access or error occurred:", error);
            alert("Connection rejected.");
        }
    } else {
        // MetaMask is not installed
        alert("MetaMask is not installed. Please install it to use this feature.");
        window.open('https://metamask.io', '_blank');
    }
}