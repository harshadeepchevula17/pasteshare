import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import CreatePastePage from "./CreatePastePage.jsx";
import PasteCreatedPage from "./PasteCreatedPage.jsx";

const createdPaste = {
  id: "0123456789abcdefghijkL",
  shareUrl: "https://paste.example/p/0123456789abcdefghijkL",
  createdAt: "2026-11-10T10:00:00Z",
  expiresAt: null,
  maxViews: 3,
};

function renderCreatePage() {
  return render(
    <MemoryRouter initialEntries={["/"]}>
      <Routes>
        <Route path="/" element={<CreatePastePage />} />
        <Route path="/created/:id" element={<PasteCreatedPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("create paste flow", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("rejects empty content without calling the API", async () => {
    renderCreatePage();
    fireEvent.click(screen.getByRole("button", { name: /create your paste/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Enter some text");
    expect(fetch).not.toHaveBeenCalled();
  });

  it("rejects non-positive view limits before sending a request", async () => {
    renderCreatePage();
    fireEvent.change(screen.getByLabelText("Your text"), { target: { value: "Hello" } });
    fireEvent.change(screen.getByLabelText(/Maximum views/), { target: { value: "0" } });
    fireEvent.click(screen.getByRole("button", { name: /create your paste/i }));
    expect(await screen.findByRole("alert")).toHaveTextContent("positive whole number");
    expect(fetch).not.toHaveBeenCalled();
  });

  it("submits content and optional settings, then shows and copies the share URL", async () => {
    fetch.mockResolvedValue({
      ok: true,
      json: async () => createdPaste,
    });
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
    });
    renderCreatePage();

    fireEvent.change(screen.getByLabelText("Your text"), { target: { value: "Hello from test" } });
    fireEvent.change(screen.getByLabelText(/Maximum views/), { target: { value: "3" } });
    fireEvent.click(screen.getByRole("button", { name: /create your paste/i }));

    expect(await screen.findByLabelText("Share URL")).toHaveValue(createdPaste.shareUrl);
    expect(fetch).toHaveBeenCalledWith(
      "/api/v1/pastes",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ content: "Hello from test", maxViews: 3 }),
      }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Copy link" }));
    await waitFor(() => expect(navigator.clipboard.writeText).toHaveBeenCalledWith(createdPaste.shareUrl));
    expect(await screen.findByRole("button", { name: "Copied" })).toBeInTheDocument();
  });

  it("sends an optional expiration as an ISO timestamp", async () => {
    fetch.mockResolvedValue({ ok: true, json: async () => createdPaste });
    renderCreatePage();
    fireEvent.change(screen.getByLabelText("Your text"), { target: { value: "Scheduled note" } });
    fireEvent.change(screen.getByLabelText("Expiration Optional"), {
      target: { value: "2099-12-31T12:00" },
    });
    fireEvent.click(screen.getByRole("button", { name: /create your paste/i }));
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    const requestBody = JSON.parse(fetch.mock.calls[0][1].body);
    expect(requestBody.expiresAt).toBe(new Date("2099-12-31T12:00").toISOString());
  });
});
