import { useEffect, useRef, type RefObject } from 'react'

export function useInfiniteScroll(
  onVisible: () => void,
  options: { disabled: boolean; root?: Element | null },
): RefObject<HTMLDivElement | null> {
  const { disabled, root = null } = options
  const ref = useRef<HTMLDivElement | null>(null)
  const onVisibleRef = useRef(onVisible)
  onVisibleRef.current = onVisible

  useEffect(() => {
    if (disabled) {
      return
    }
    const node = ref.current
    if (!node) {
      return
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          onVisibleRef.current()
        }
      },
      { root, rootMargin: '240px', threshold: 0 },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [disabled, root])

  return ref
}
