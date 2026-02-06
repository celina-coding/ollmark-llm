import "./style.css";

type Json = Record<string, any>;

const WS_URL = "ws://localhost:4401/plugin";
const API_BASE = "http://localhost:4401";

let ws: WebSocket | null = null;
let chatReady = false;

let initInFlight: Promise<void> | null = null;

function $(id: string) {
  return document.getElementById(id) as HTMLElement | null;
}

function setWsStatus(connected: boolean) {
  const dot = $("wsStatusDot");
  if (!dot) return;

  dot.classList.remove("connected", "disconnected");
  dot.classList.add(connected ? "connected" : "disconnected");
}

function log(line: string) {
  const el = $("logs") as HTMLPreElement | null;
  if (!el) return;
  const ts = new Date().toLocaleTimeString();
  el.textContent += `[${ts}] ${line}\n`;
  el.scrollTop = el.scrollHeight;
}

function setText(id: string, text: string) {
  const el = $(id);
  if (el) el.textContent = text;
}

function appendChat(who: "me" | "ai", text: string) {
  const box = $("chatBox");
  if (!box) return;

  const div = document.createElement("div");
  div.className = `bubble ${who}`;

  const ts = new Date().toLocaleTimeString();

  const header = document.createElement("div");
  header.className = "bubble-head";
  header.textContent = `${who === "me" ? "Vous" : "IA"} • ${ts}`;

  const body = document.createElement("div");
  body.className = "bubble-body";
  body.textContent = text;

  div.appendChild(header);
  div.appendChild(body);

  box.appendChild(div);
  box.scrollTop = box.scrollHeight;
}

function resetChatUi() {
  chatReady = false;

  const cidEl = $("conversationId") as HTMLInputElement | null;
  if (cidEl) cidEl.value = "";

  const chatBox = $("chatBox");
  if (chatBox) chatBox.innerHTML = "";

  const msgEl = $("chatMsg") as HTMLInputElement | null;
  if (msgEl) {
    msgEl.value = "";
    msgEl.disabled = true;
  }

  const sendBtn = $("sendChatBtn") as HTMLButtonElement | null;
  if (sendBtn) sendBtn.disabled = true;
}

async function postJson<T = any>(url: string, body?: Json): Promise<T> {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await res.text();
  let data: any = {};
  try {
    data = text ? JSON.parse(text) : {};
  } catch {}

  if (!res.ok) throw new Error(data?.error || `${res.status} ${res.statusText}`);
  return data as T;
}

// j'ai plus besoin de toi déjà personne ne t'aime deleteReq

/*async function deleteReq(url: string): Promise<any> {
  const res = await fetch(url, { method: "DELETE" });

  const text = await res.text();
  let data: any = {};
  try {
    data = text ? JSON.parse(text) : {};
  } catch {}

  if (!res.ok) throw new Error(data?.error || `${res.status} ${res.statusText}`);
  return data;
} 
  */

/**
 * Init automatique:
 * - reset chat
 * - crée une conversation
 */
async function autoInitChat() {
  // si déjà en cours, ne relance pas
  if (initInFlight) return initInFlight;

  initInFlight = (async () => {
    try {
      // crée une conversation automatiquement
      await newConversation();
    } finally {
      initInFlight = null;
    }
  })();

  return initInFlight;
}

function connectWs() {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    log("WebSocket already open/connecting");
    return;
  }

  log(`Connecting WS: ${WS_URL}`);
  setWsStatus(false);

  ws = new WebSocket(WS_URL);

  ws.onopen = async () => {
    log("WebSocket connected");
    setWsStatus(true);

    //auto conversation dès l’ouverture (une seule fois)
    try {
      await autoInitChat();
    } catch (e: any) {
      log(`Auto init chat failed: ${e?.message || String(e)}`);
    }
  };

  ws.onerror = () => {
    log("WebSocket erreur (check les logs serveur)");
    setWsStatus(false);
  };

  ws.onclose = () => {
    log("WebSocket fermé");
    setWsStatus(false);

    resetChatUi();
  };

  ws.onmessage = (event) => {
    log(`WebSocket message: ${event.data}`);

    //forward EXACT du JSON vers le runtime penpot
    try {
      const request = JSON.parse(event.data);
      window.parent.postMessage(request, "*");
    } catch {
      log("WS message not JSON (ignored)");
    }
  };
}

window.addEventListener("message", (event) => {
  const msg = event.data;
  if (!msg || typeof msg !== "object") return;

  if (msg.type === "task-response") {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      log("Cannot send task-response: WS not open");
      return;
    }

    const payload = (msg as any).response ?? msg;
    ws.send(JSON.stringify(payload));
    log(`Sent task-response id=${payload?.id ?? "?"}`);
  }
});

async function runJs() {
  const codeEl = $("jsCode") as HTMLTextAreaElement | null;
  const code = codeEl?.value ?? "";
  log(`Calling POST ${API_BASE}/mcp/execute-code (len=${code.length})`);

  try {
    const data = await postJson(`${API_BASE}/mcp/execute-code`, { code });
    log(`execute-code success=${data?.success}`);

    if (data?.logs) log(`logs:\n${data.logs}`);
    if (data?.result !== undefined) log(`result: ${JSON.stringify(data.result)}`);
  } catch (e: any) {
    log(`ERROR execute-code: ${e?.message || String(e)}`);
  }
}

async function newConversation() {

  resetChatUi();

  log(`Calling POST ${API_BASE}/mcp/chat/new`);
  try {
    const data = await postJson(`${API_BASE}/mcp/chat/new`, {});
    const cid = data?.conversationId as string | undefined;
    if (!cid) throw new Error("No conversationId in response");

    const cidEl = $("conversationId") as HTMLInputElement | null;
    if (cidEl) cidEl.value = cid;

    chatReady = true;

    const msgEl = $("chatMsg") as HTMLInputElement | null;
    const sendBtn = $("sendChatBtn") as HTMLButtonElement | null;
    if (msgEl) msgEl.disabled = false;
    if (sendBtn) sendBtn.disabled = false;

    log(`New conversationId=${cid}`);
    appendChat("ai", `Conversation ready: ${cid}`);
  } catch (e: any) {
    const err = e?.message || String(e);
    log(`ERROR new conversation: ${err}`);
    appendChat("ai", `ERROR new conversation: ${err}`);
  }
}

function resetConversation() {
  const chatBox = $("chatBox");
  if (chatBox) chatBox.innerHTML = "";

  const msgEl = $("chatMsg") as HTMLInputElement | null;
  if (msgEl) msgEl.value = "";

  chatReady = true;

  log("Conversation reset (same conversationId)");
}


async function sendChat() {
  const cidEl = $("conversationId") as HTMLInputElement | null;
  const msgEl = $("chatMsg") as HTMLInputElement | null;

  const conversationId = cidEl?.value?.trim() ?? "";
  const message = msgEl?.value?.trim() ?? "";

  if (!chatReady || !conversationId) {
    log("Chat not ready. Create conversation first.");
    return;
  }

  if (!message) return;

  appendChat("me", message);
  if (msgEl) msgEl.value = "";

  log(`Calling POST ${API_BASE}/mcp/chat (cid=${conversationId}, len=${message.length})`);

  try {
    const data = await postJson(`${API_BASE}/mcp/chat`, { conversationId, message });
    const response = (data?.response as string) ?? "";
    appendChat("ai", response.trim() ? response : "(empty AI response)");
    log("Chat OK");
  } catch (e: any) {
    const err = e?.message || String(e);
    log(`ERROR chat: ${err}`);
    appendChat("ai", `ERROR chat: ${err}`);
  }
}

function clearLogs() {
  const el = $("logs") as HTMLPreElement | null;
  if (el) el.textContent = "";
}

function setMode(mode: "chat" | "js") {
  const chatPanel = $("chatPanel");
  const jsPanel = $("jsPanel");

  if (mode === "chat") {
    if (chatPanel) chatPanel.style.display = "";
    if (jsPanel) jsPanel.style.display = "none";
  } else {
    if (chatPanel) chatPanel.style.display = "none";
    if (jsPanel) jsPanel.style.display = "";
  }
}

function wireUi() {
  setText("wsUrl", WS_URL);
  setText("apiUrl", API_BASE);

  const modeSelect = $("modeSelect") as HTMLSelectElement | null;
  modeSelect?.addEventListener("change", () => {
    const v = (modeSelect.value as "chat" | "js") || "chat";
    setMode(v);
  });
  setMode(((modeSelect?.value as any) ?? "chat") as "chat" | "js");

  $("connectBtn")?.addEventListener("click", connectWs);
  $("runJsBtn")?.addEventListener("click", runJs);

  $("clearLogsBtn")?.addEventListener("click", clearLogs);
  $("clearLogsBtn2")?.addEventListener("click", clearLogs);

  $("newConvBtn")?.addEventListener("click", newConversation);
  $("sendChatBtn")?.addEventListener("click", sendChat);

  $("newConvBtn2")?.addEventListener("click", (e) => {
    e.preventDefault();
    newConversation();
  });

  $("deleteConvBtn")?.addEventListener("click", resetConversation);

  $("deleteConvBtn2")?.addEventListener("click", (e) => {
  e.preventDefault();
  resetConversation();
  });


  const msgEl = $("chatMsg") as HTMLInputElement | null;
  msgEl?.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      sendChat();
    }
  });

  setWsStatus(false);
  resetChatUi();
  connectWs();
}

wireUi();
