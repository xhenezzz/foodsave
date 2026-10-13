import { useEffect, useRef } from "react";
import type { Listing } from "./types";

let leafletPromise: Promise<any> | null = null;
function loadLeaflet() {
  if ((window as any).L) return Promise.resolve((window as any).L);
  if (leafletPromise) return leafletPromise;
  leafletPromise = new Promise((resolve, reject) => {
    if (!document.querySelector('link[data-foodsave-leaflet]')) {
      const css = document.createElement("link");
      css.rel = "stylesheet";
      css.dataset.foodsaveLeaflet = "true";
      css.href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css";
      document.head.appendChild(css);
    }
    const script = document.createElement("script");
    script.src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";
    script.onload = () => resolve((window as any).L);
    script.onerror = () => { leafletPromise = null; reject(new Error("Не удалось загрузить карту")); };
    document.body.appendChild(script);
  });
  return leafletPromise;
}

export function MapView({ user, listings, recommended = false }: { user: { lat: number; lon: number }; listings: Listing[]; recommended?: boolean }) {
  const element = useRef<HTMLDivElement>(null);
  useEffect(() => {
    let alive = true;
    let map: any;
    let resize: ReturnType<typeof setTimeout> | undefined;
    loadLeaflet().then((L: any) => {
      if (!alive || !element.current) return;
      map = L.map(element.current).setView([user.lat, user.lon], 13);
      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", { attribution: "© OpenStreetMap contributors" }).addTo(map);
      const userMarker = L.circleMarker([user.lat, user.lon], { radius: 8, color: "#173c24", fillColor: "#65a54f", fillOpacity: 1 }).addTo(map).bindPopup("Вы здесь");
      const markers: any[] = [userMarker];
      listings.forEach((listing, index) => {
        if (!Number.isFinite(listing.latitude) || !Number.isFinite(listing.longitude)) return;
        const highlighted = recommended && index < 3;
        const marker = highlighted
          ? L.circleMarker([listing.latitude!, listing.longitude!], { radius: 12, color: "#244c2d", weight: 3, fillColor: "#cfe873", fillOpacity: 1 }).addTo(map)
          : L.marker([listing.latitude!, listing.longitude!]).addTo(map);
        const popup = document.createElement("div");
        const title = document.createElement("strong");
        title.textContent = listing.businessName ?? "Бизнес";
        const detail = document.createElement("p");
        detail.textContent = listing.title;
        if (highlighted) {
          const highlight = document.createElement("p");
          highlight.textContent = `★ Топ-${index+1} по рекомендациям`;
          popup.append(highlight);
        }
        popup.append(title, detail);
        marker.bindPopup(popup); // textContent вместо небезопасного HTML из API
        markers.push(marker);
      });
      if (markers.length > 1) map.fitBounds(L.featureGroup(markers).getBounds().pad(0.2));
      resize = setTimeout(() => map?.invalidateSize(), 120);
    }).catch(() => { if (element.current) element.current.textContent = "Карта недоступна. Предложения показаны ниже."; });
    return () => { alive = false; if (resize) clearTimeout(resize); map?.remove(); };
  }, [user.lat, user.lon, listings, recommended]);
  return <div className="real-map" ref={element}><div className="map-loading">Загружаю карту…</div></div>;
}
