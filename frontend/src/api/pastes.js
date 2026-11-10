const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/+$/, "");

async function request(path, options) {
  let response;
  try {
    response = await fetch(`${API_BASE_URL}/api/v1/pastes${path}`, {
      ...options,
      signal: options?.signal,
      headers: {
        Accept: "application/json",
        ...options?.headers,
      },
    });
  } catch {
    throw new Error("Unable to reach the server. Check your connection and try again.");
  }

  const body = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(body?.message ?? "The request could not be completed.");
    error.status = response.status;
    throw error;
  }
  return body;
}

export function createPaste(paste) {
  return request("", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(paste),
  });
}

export function getPaste(id, options = {}) {
  return request(`/${encodeURIComponent(id)}`, { method: "GET", signal: options.signal });
}
