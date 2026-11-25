import { useListingCover } from "./ListingPhotos";
import { FoodCover } from "../visuals";
import type { FoodCategory } from "../types";

export function BusinessItemCover({listingId, title, category}: {listingId: string; title: string; category?: FoodCategory | null}) {
  const photo = useListingCover(listingId);
  return <div className="business-item-avatar">
    {photo ? <img src={photo} alt={title} className="business-item-photo"/> : <FoodCover title={title} category={category} compact/>}
  </div>;
}
