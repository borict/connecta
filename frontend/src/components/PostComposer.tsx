import { useEffect, useRef, useState, type FormEvent } from 'react'
import { createPost } from '../api/posts'
import { errorMessage } from '../api/errorMessage'
import type { FeedPostDto } from '../types/api'

const MAX_CONTENT = 500
const MAX_IMAGE_BYTES = 5 * 1024 * 1024

type PostComposerProps = {
  onCreated: (post: FeedPostDto) => void
}

export function PostComposer({ onCreated }: PostComposerProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [content, setContent] = useState('')
  const [image, setImage] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const previewUrl = image ? URL.createObjectURL(image) : null

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl)
      }
    }
  }, [previewUrl])

  function resetForm() {
    setContent('')
    setImage(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  function handleImageChange(file: File | null) {
    setError(null)
    if (!file) {
      setImage(null)
      return
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setError('Image must be at most 5MB')
      setImage(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
      return
    }
    setImage(file)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmed = String(new FormData(event.currentTarget).get('content') ?? content).trim()
    if (!trimmed) {
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      const post = await createPost(trimmed, image)
      onCreated(post)
      resetForm()
    } catch (err) {
      setError(errorMessage(err, 'Could not create post'))
    } finally {
      setSubmitting(false)
    }
  }

  const remaining = MAX_CONTENT - content.length

  return (
    <form className="card shadow-sm mb-3" onSubmit={handleSubmit}>
      <div className="card-body">
        {error ? <div className="alert alert-danger py-2">{error}</div> : null}
        <label className="form-label visually-hidden" htmlFor="postContent">
          What’s on your mind?
        </label>
        <textarea
          id="postContent"
          name="content"
          className="form-control"
          rows={3}
          maxLength={MAX_CONTENT}
          placeholder="What’s on your mind?"
          value={content}
          onChange={(event) => setContent(event.target.value)}
          required
        />
        {previewUrl ? (
          <img src={previewUrl} alt="" className="mt-3 rounded w-100" style={{ maxHeight: 240, objectFit: 'cover' }} />
        ) : null}
        <div className="d-flex flex-wrap align-items-center gap-2 mt-3">
          <input
            ref={fileInputRef}
            id="postImage"
            type="file"
            className="form-control form-control-sm"
            style={{ maxWidth: 260 }}
            accept="image/jpeg,image/png,image/webp"
            onChange={(event) => handleImageChange(event.target.files?.[0] ?? null)}
          />
          <span className={`small ms-auto ${remaining <= 20 ? 'text-danger' : 'text-secondary'}`}>
            {remaining}
          </span>
          <button className="btn btn-primary" type="submit" disabled={submitting}>
            {submitting ? 'Posting…' : 'Post'}
          </button>
        </div>
        <div className="form-text">JPEG, PNG, or WebP. Max 5MB. Optional.</div>
      </div>
    </form>
  )
}
