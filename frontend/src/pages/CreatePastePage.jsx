import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createPaste } from "../api/pastes.js";

const MAX_CONTENT_LENGTH = Number(import.meta.env.VITE_MAX_CONTENT_LENGTH ?? 100000);

export default function CreatePastePage() {
  const navigate = useNavigate();
  const [content, setContent] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [maxViews, setMaxViews] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");

    if (!content.trim()) {
      setError("Enter some text before creating a paste.");
      return;
    }
    if (content.length > MAX_CONTENT_LENGTH) {
      setError(`Text must be ${MAX_CONTENT_LENGTH.toLocaleString()} characters or fewer.`);
      return;
    }

    let expiration;
    if (expiresAt) {
      const parsedExpiration = new Date(expiresAt);
      if (Number.isNaN(parsedExpiration.getTime()) || parsedExpiration.getTime() <= Date.now()) {
        setError("Choose an expiration time in the future.");
        return;
      }
      expiration = parsedExpiration.toISOString();
    }

    let parsedMaxViews;
    if (maxViews !== "") {
      parsedMaxViews = Number(maxViews);
      if (!Number.isSafeInteger(parsedMaxViews) || parsedMaxViews <= 0) {
        setError("Maximum views must be a positive whole number.");
        return;
      }
    }

    const request = { content };
    if (expiration) request.expiresAt = expiration;
    if (parsedMaxViews !== undefined) request.maxViews = parsedMaxViews;

    setIsSubmitting(true);
    try {
      const result = await createPaste(request);
      navigate(`/created/${encodeURIComponent(result.id)}`, { state: { paste: result } });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="create-layout">
      <div className="intro">
        <p className="eyebrow">A QUIET PLACE FOR YOUR TEXT</p>
        <h1>Share a thought, not an account.</h1>
        <p>
          Drop in notes, snippets, or plain text. Get one simple link to share
          it with anyone.
        </p>
      </div>

      <form className="panel create-form" onSubmit={handleSubmit} noValidate>
        <div className="form-heading">
          <div>
            <h2>New paste</h2>
            <p>Your text is displayed as plain text.</p>
          </div>
          <span className="private-badge">UNLISTED</span>
        </div>

        <label className="field-label" htmlFor="paste-content">
          Your text
        </label>
        <textarea
          id="paste-content"
          className="text-editor"
          value={content}
          onChange={(event) => setContent(event.target.value)}
          placeholder="Write or paste something here…"
          maxLength={MAX_CONTENT_LENGTH}
          aria-describedby="content-hint"
          required
        />
        <div className="editor-meta" id="content-hint">
          <span>Plain text only</span>
          <span>
            {content.length.toLocaleString()} / {MAX_CONTENT_LENGTH.toLocaleString()}
          </span>
        </div>

        <div className="options-grid">
          <div className="field-group">
            <label className="field-label" htmlFor="expires-at">
              Expiration <span className="optional">Optional</span>
            </label>
            <input
              id="expires-at"
              type="datetime-local"
              value={expiresAt}
              onChange={(event) => setExpiresAt(event.target.value)}
            />
            <span className="field-hint">Leave blank to keep it available.</span>
          </div>
          <div className="field-group">
            <label className="field-label" htmlFor="max-views">
              Maximum views <span className="optional">Optional</span>
            </label>
            <input
              id="max-views"
              type="number"
              min="1"
              step="1"
              inputMode="numeric"
              value={maxViews}
              onChange={(event) => setMaxViews(event.target.value)}
              placeholder="No limit"
            />
            <span className="field-hint">The last allowed view still works.</span>
          </div>
        </div>

        {error && (
          <p className="alert alert-error" role="alert">
            {error}
          </p>
        )}

        <button className="button button-primary submit-button" type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Creating…" : "Create your paste"}
          {!isSubmitting && <span aria-hidden="true">→</span>}
        </button>
        <p className="privacy-note">
          Anyone with the link can read your paste. Don’t share sensitive
          information.
        </p>
      </form>
    </section>
  );
}
