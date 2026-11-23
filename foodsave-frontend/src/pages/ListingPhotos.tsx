import { useEffect, useState } from "react";
import { api } from "../api";
import type { ListingImage } from "../types";

export function useListingCover(listingId:string, updateKey=0) {
  const [url,setUrl]=useState<string|null>(null);
  useEffect(()=>{
    let cancelled=false; let objectUrl:string|null=null;
    async function load(){
      setUrl(null);
      try {
        const images=await api.getListingImages(listingId);
        if (!images.length || cancelled) return;
        const blob=await api.getListingImageBlob(listingId, images[0].id);
        if(cancelled) return;
        objectUrl=URL.createObjectURL(blob);setUrl(objectUrl);
      } catch { /* Emoji fallback for listings without photos / unauthenticated users */ }
    }
    void load();
    return ()=>{cancelled=true;if(objectUrl)URL.revokeObjectURL(objectUrl);};
  },[listingId,updateKey]);
  return url;
}

export function PhotoManager({listingId,onRemove}:{listingId:string;onRemove?:(imageId:string)=>void}) {
  const [photos,setPhotos]=useState<Array<ListingImage & {url?:string}>>([]);
  const [error,setError]=useState("");
  useEffect(()=>{
    let alive=true; const urls:string[]=[];
    api.getListingImages(listingId).then(async images=>{
      const result=await Promise.all(images.map(async img=>{
        try {const blob=await api.getListingImageBlob(listingId,img.id);const url=URL.createObjectURL(blob);urls.push(url);return {...img,url};}
        catch{return img;}
      }));
      if(alive)setPhotos(result);
    }).catch(e=>{if(alive)setError(String(e));});
    return ()=>{alive=false;urls.forEach(u=>URL.revokeObjectURL(u));};
  },[listingId]);
  return <div className="photo-manager">
    {photos.map(img=><div className="photo-manager-item" key={img.id}>
      {img.url?<img src={img.url} alt="Загруженный продукт"/>:<span>Фото</span>}
      <button type="button" onClick={async()=>{
        if(!window.confirm("Удалить эту фотографию?"))return;
        try {await api.deleteListingImage(listingId,img.id);setPhotos(x=>x.filter(p=>p.id!==img.id));onRemove?.(img.id);}
        catch(e){setError(String(e));}
      }}>Удалить</button>
    </div>)}
    {error&&<p role="alert">{error}</p>}
  </div>;
}
