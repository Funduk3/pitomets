import { useEffect, useMemo, useState } from 'react';
import { resolveApiUrl } from '../api/axios';

export const PhotoCarousel = ({
  photos = [],
  imageClassName,
  imageStyle,
  containerStyle,
  emptyLabel = 'Нет фото',
}) => {
  const normalizedPhotos = useMemo(
    () => (Array.isArray(photos) ? photos.filter(Boolean) : []),
    [photos]
  );
  const [index, setIndex] = useState(0);

  useEffect(() => {
    setIndex(0);
  }, [normalizedPhotos.length]);

  if (normalizedPhotos.length === 0) {
    return (
      <div
        style={{
          background: '#f5f5f5',
          borderRadius: '6px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#999',
          ...containerStyle,
        }}
      >
        {emptyLabel}
      </div>
    );
  }

  const total = normalizedPhotos.length;
  const showControls = total > 1;
  const currentUrl = resolveApiUrl(normalizedPhotos[index]);

  const goPrev = (event) => {
    event?.preventDefault?.();
    setIndex((prev) => (prev - 1 + total) % total);
  };

  const goNext = (event) => {
    event?.preventDefault?.();
    setIndex((prev) => (prev + 1) % total);
  };

  return (
    <div
      style={{
        position: 'relative',
        width: '100%',
        ...containerStyle,
      }}
    >
      <img
        src={currentUrl}
        alt={`Фото ${index + 1}`}
        className={imageClassName}
        style={imageStyle}
      />
      {showControls && (
        <>
          <button
            type="button"
            onClick={goPrev}
            style={{
              position: 'absolute',
              top: '50%',
              left: '8px',
              transform: 'translateY(-50%)',
              width: '28px',
              height: '28px',
              borderRadius: '50%',
              border: 'none',
              background: 'rgba(0,0,0,0.55)',
              color: '#fff',
              cursor: 'pointer',
            }}
            aria-label="Предыдущее фото"
          >
            ‹
          </button>
          <button
            type="button"
            onClick={goNext}
            style={{
              position: 'absolute',
              top: '50%',
              right: '8px',
              transform: 'translateY(-50%)',
              width: '28px',
              height: '28px',
              borderRadius: '50%',
              border: 'none',
              background: 'rgba(0,0,0,0.55)',
              color: '#fff',
              cursor: 'pointer',
            }}
            aria-label="Следующее фото"
          >
            ›
          </button>
          <div
            style={{
              position: 'absolute',
              right: '10px',
              bottom: '8px',
              padding: '2px 6px',
              borderRadius: '999px',
              background: 'rgba(0,0,0,0.55)',
              color: '#fff',
              fontSize: '0.72rem',
              lineHeight: 1.2,
            }}
          >
            {index + 1}/{total}
          </div>
        </>
      )}
    </div>
  );
};
