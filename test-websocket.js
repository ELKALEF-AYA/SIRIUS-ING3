const SockJS = require('sockjs-client');
const Stomp = require('stompjs');

const WS_URL = 'http://localhost:8081/ws';
const CLIENT_ID = 25;
const AGENT_ID = 1;
const CONVERSATION_ID = 1;

function createMessage(senderId, senderType, receiverId, content, conversationId) {
    return {
        senderId: senderId,
        senderType: senderType,
        receiverId: receiverId,
        content: content,
        conversationId: conversationId,
        timestamp: new Date().toISOString()
    };
}

function connectClientWebSocket(clientId, agentId, conversationId) {
    return new Promise((resolve, reject) => {
        const socket = new SockJS(WS_URL);
        const stompClient = Stomp.over(socket);

        stompClient.connect({}, function () {
            console.log('[CLIENT] CONNECTE au WebSocket');

            stompClient.subscribe('/topic/client/' + clientId, function (message) {
                const receivedMessage = JSON.parse(message.body);
                console.log('\n[CLIENT] REÇOIT message de l\'agent:');
                console.log('   "' + receivedMessage.content + '"');
            });

            resolve({ stompClient, clientId, agentId, conversationId });
        }, function (error) {
            console.error('[CLIENT] Erreur:', error);
            reject(error);
        });
    });
}

function connectAgentWebSocket(agentId, conversationId) {
    return new Promise((resolve, reject) => {
        const socket = new SockJS(WS_URL);
        const stompClient = Stomp.over(socket);

        stompClient.connect({}, function () {
            console.log('[AGENT] CONNECTE au WebSocket');

            stompClient.subscribe('/topic/agent/' + agentId, function (message) {
                const receivedMessage = JSON.parse(message.body);
                console.log('\n[AGENT] REÇOIT message du client:');
                console.log('   "' + receivedMessage.content + '"');
            });

            resolve({ stompClient, agentId });
        }, function (error) {
            console.error('[AGENT] Erreur:', error);
            reject(error);
        });
    });
}

async function runTest() {
    console.log('===== TEST WEBSOCKET CHAT =====\n');

    try {
        console.log('[1] Connexion...');
        const clientConn = await connectClientWebSocket(CLIENT_ID, AGENT_ID, CONVERSATION_ID);
        const agentConn = await connectAgentWebSocket(AGENT_ID, CONVERSATION_ID);

        await new Promise(resolve => setTimeout(resolve, 500));

        console.log('\n[2] Client envoie message...');
        const msg1 = createMessage(CLIENT_ID, 'CLIENT', AGENT_ID, 'Bonjour, je veux recevoir ma facture.', CONVERSATION_ID);
        clientConn.stompClient.send('/app/chat.send', {}, JSON.stringify(msg1));

        await new Promise(resolve => setTimeout(resolve, 500));

        console.log('\n[3] Agent repond...');
        const msg2 = createMessage(AGENT_ID, 'AGENT', CLIENT_ID, 'Vous cherchez la facture de quel mois  ?', CONVERSATION_ID);
        agentConn.stompClient.send('/app/chat.send', {}, JSON.stringify(msg2));

        await new Promise(resolve => setTimeout(resolve, 500));

        console.log('\n[4] Client repond...');
        const msg3 = createMessage(CLIENT_ID, 'CLIENT', AGENT_ID, 'Du mois Mars .', CONVERSATION_ID);
        clientConn.stompClient.send('/app/chat.send', {}, JSON.stringify(msg3));

        await new Promise(resolve => setTimeout(resolve, 1000));

        console.log('\n===== TEST TERMINE =====');

        clientConn.stompClient.disconnect();
        agentConn.stompClient.disconnect();

    } catch (error) {
        console.error('ERREUR:', error.message);
        process.exit(1);
    }
}

runTest();