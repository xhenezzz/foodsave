import { useEffect, useRef, useState } from "react";
import type { City } from "./types";
import { loadLeaflet } from "./MapView";

export type BusinessLocation = {
  latitude: number;
  longitude: number;
} | null;

const CITY_CENTERS: Record<City, [number, number]> = {
  ALMATY: [43.238949, 76.889709],
  ASTANA: [51.169392, 71.449074],
};

function isValidLocation(value: BusinessLocation): value is { latitude: number; longitude: number } {
  return Boolean(
    value &&
    Number.isFinite(value.latitude) &&
    Number.isFinite(value.longitude) &&
    value.latitude >= -90 &&
    value.latitude <= 90 &&
    value.longitude >= -180 &&
    value.longitude <= 180
  );
}

export function BusinessLocationPicker({
  city,
  value,
  onChange,
}: {
  city: City;
  value: BusinessLocation;
  onChange: (value: BusinessLocation) => void;
}) {
  const elementRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<any>(null);
  const markerRef = useRef<any>(null);
  const syncMarkerRef = useRef<(next: BusinessLocation) => void>(() => undefined);
  const onChangeRef = useRef(onChange);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  useEffect(() => {
    let alive = true;
    let map: any;
    let resizeTimer: number | undefined;

    setLoading(true);
    setError("");

    loadLeaflet()
      .then((L: any) => {
        if (!alive || !elementRef.current) return;

        const hasSavedLocation = isValidLocation(value);
        const center: [number, number] = hasSavedLocation
          ? [value.latitude, value.longitude]
          : CITY_CENTERS[city];

        map = L.map(elementRef.current, {
          scrollWheelZoom: true,
        }).setView(center, hasSavedLocation ? 16 : 12);

        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
          attribution: "© OpenStreetMap contributors",
          maxZoom: 19,
        }).addTo(map);

        const placeMarker = (next: BusinessLocation) => {
          if (!next) {
            markerRef.current?.remove();
            markerRef.current = null;
            return;
          }

          const position: [number, number] = [next.latitude, next.longitude];

          if (!markerRef.current) {
            markerRef.current = L.marker(position, { draggable: true }).addTo(map);
            markerRef.current.bindPopup("Местоположение вашего заведения");
            markerRef.current.on("dragend", () => {
              const point = markerRef.current?.getLatLng();
              if (!point) return;
              onChangeRef.current({
                latitude: Number(point.lat.toFixed(7)),
                longitude: Number(point.lng.toFixed(7)),
              });
            });
          } else {
            markerRef.current.setLatLng(position);
          }
        };

        syncMarkerRef.current = placeMarker;

        map.on("click", (event: any) => {
          const next = {
            latitude: Number(event.latlng.lat.toFixed(7)),
            longitude: Number(event.latlng.lng.toFixed(7)),
          };
          placeMarker(next);
          onChangeRef.current(next);
        });

        if (hasSavedLocation) placeMarker(value);
        mapRef.current = map;

        requestAnimationFrame(() => map?.invalidateSize());
        resizeTimer = window.setTimeout(() => map?.invalidateSize(), 180);
        setLoading(false);
      })
      .catch(() => {
        if (!alive) return;
        setError("Не удалось загрузить карту. Проверь подключение к интернету и обнови страницу.");
        setLoading(false);
      });

    return () => {
      alive = false;
      if (resizeTimer !== undefined) window.clearTimeout(resizeTimer);
      map?.remove();
      mapRef.current = null;
      markerRef.current = null;
      syncMarkerRef.current = () => undefined;
    };
    // Initial coordinates are applied during map creation and synchronized below.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [city]);

  useEffect(() => {
    if (!mapRef.current) return;
    syncMarkerRef.current(value);
  }, [value?.latitude, value?.longitude]);

  return (
    <section className="business-location-picker">
      <div className="business-location-heading">
        <div>
          <b>Местоположение на карте</b>
          <p>Нажми на карту, чтобы поставить метку. Её можно перетащить мышкой.</p>
        </div>
        {value && (
          <button type="button" className="business-location-reset" onClick={() => onChange(null)}>
            Сбросить метку
          </button>
        )}
      </div>

      <div className="business-location-map">
        <div ref={elementRef} className="business-location-map-canvas" />
        {loading && <div className="business-location-overlay">Загружаю карту…</div>}
        {error && <div className="business-location-overlay business-location-error" role="alert">{error}</div>}
      </div>

      <div className="business-location-coordinates" aria-live="polite">
        <span>Широта <b>{value ? value.latitude.toFixed(6) : "—"}</b></span>
        <span>Долгота <b>{value ? value.longitude.toFixed(6) : "—"}</b></span>
      </div>
    </section>
  );
}
