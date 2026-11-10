import { Link, Route, Routes } from "react-router-dom";
import CreatePastePage from "./pages/CreatePastePage.jsx";
import PasteCreatedPage from "./pages/PasteCreatedPage.jsx";
import ViewPastePage from "./pages/ViewPastePage.jsx";

function Layout({ children }) {
  return (
    <div className="app-shell">
      <header className="topbar">
        <Link className="brand" to="/" aria-label="PasteShare home">
          <span className="brand-mark" aria-hidden="true">
            P
          </span>
          <span>PasteShare</span>
        </Link>
        <span className="topbar-note">Simple, private text sharing</span>
      </header>
      <main className="main-content">{children}</main>
      <footer className="footer">Text is shared by link, not by account.</footer>
    </div>
  );
}

function NotFoundPage() {
  return (
    <section className="panel state-panel">
      <p className="eyebrow">PAGE NOT FOUND</p>
      <h1>This page isn’t here.</h1>
      <Link className="button button-primary" to="/">
        Create a paste
      </Link>
    </section>
  );
}

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<CreatePastePage />} />
        <Route path="/created/:id" element={<PasteCreatedPage />} />
        <Route path="/p/:id" element={<ViewPastePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Layout>
  );
}
