import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import ViewPastePage from "./ViewPastePage.jsx";

function renderView(id = "paste-id") {
  return render(
    <MemoryRouter initialEntries={[`/p/${id}`]}>
      <Routes>
        <Route path="/p/:id" element={<ViewPastePage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("paste retrieval", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("shows loading and renders returned text literally", async () => {
    let finishRequest;
    fetch.mockReturnValue(new Promise((resolve) => {
      finishRequest = resolve;
    }));
    renderView();
    expect(screen.getByText("Loading your paste…")).toBeInTheDocument();

    finishRequest({
      ok: true,
      json: async () => ({
        id: "paste-id",
        content: "<img src=x onerror=alert(1)>",
        createdAt: "2026-11-10T10:00:00Z",
        expiresAt: null,
        viewsRemaining: 2,
      }),
    });

    expect(await screen.findByText("<img src=x onerror=alert(1)>")).toBeInTheDocument();
    expect(document.querySelector("img")).toBeNull();
    expect(screen.getByText("2 views remaining")).toBeInTheDocument();
  });

  it.each([
    [404, "doesn’t exist"],
    [410, "expired or reached its view limit"],
    [500, "server"],
  ])("shows an informative message for HTTP %i", async (status) => {
    fetch.mockResolvedValue({
      ok: false,
      status,
      json: async () => ({ message: "Backend error detail" }),
    });
    renderView();
    expect(await screen.findByRole("alert")).toBeInTheDocument();
    if (status === 404) expect(screen.getByText(/doesn’t exist/)).toBeInTheDocument();
    if (status === 410) expect(screen.getByText(/expired or reached its view limit/)).toBeInTheDocument();
    if (status === 500) expect(screen.getByText(/server error.*Backend error detail/i)).toBeInTheDocument();
  });
});
