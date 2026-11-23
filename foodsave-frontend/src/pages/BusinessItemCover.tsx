import { useListingCover } from "./ListingPhotos";
export function BusinessItemCover({listingId}:{listingId:string}) {
  const photo=useListingCover(listingId);
  return <div className="business-item-avatar">{photo?<img src={photo} alt="Фото товара" className="business-item-photo"/>:"🥡"}</div>;
}
