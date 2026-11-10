import { useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";

export default function PasteCreatedPage() {
  const { id } = useParams();
  const { state } = useLocation();
  const [copied, setCopied] = useState(false);
  const [copyError, setCopyError] = useState("");
  const paste = state?.paste;
  const shareUrl = paste?.shareUrl ?? `${window.location.origin}/p/${encodeURIComponent(id)}`;

  async function copyLink() {
    setCopyError("");
    try {
      await navigator.clipboard.writeText(shareUrl);
      setCopied(true);
    } catch {
      setCopied(false);
      setCopyError("Copy wasn’t available. Select and copy the link above.");
    }
  }

  return (
    <section className="panel result-panel">
      <div className="success-icon" aria-hidden="true">✓</div>
      <p className="eyebrow">YOUR PASTE IS READY</p>
      <h1>One link. That’s it.</h1>
      <p className="muted-copy">
        Anyone with this link can view your text. Keep it somewhere safe.
      </p>

      <label className="field-label" htmlFor="share-url">
        Share URL
      </label>
      <div className="copy-row">
        <input id="share-url" className="share-input" readOnly value={shareUrl} />
        <button className="button button-secondary" type="button" onClick={copyLink}>
          {copied ? "Copied" : "Copy link"}
        </button>
      </div>
      {copyError && <p className="alert alert-error" role="alert">{copyError}</p>}

      {paste && (
        <dl className="paste-details">
          <div>
            <dt>Expires</dt>
            <dd>{paste.expiresAt ? new Date(paste.expiresAt).toLocaleString() : "Never"}</dd>
          </div>
          <div>
            <dt>View limit</dt>
            <dd>{paste.maxViews ?? "No limit"}</dd>
          </div>
        </dl>
      )}

      <div className="result-actions">
        <Link className="button button-primary" to={`/p/${encodeURIComponent(id)}`}>
          View paste
        </Link>
        <Link className="text-link" to="/">Create another</Link>
      </div>
    </section>
  );
}
