import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./App.css";
import "./production-refresh.css";
import "./animations.css";
import "./effects3d.css";
import { installEffects } from "./effects3d";

installEffects();

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);