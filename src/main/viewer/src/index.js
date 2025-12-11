import React from "react";
import { createRoot } from "react-dom/client";
import ThreeScene from "./ThreeScene";
import "./styles.css";

function App() {
  return (
    <div className="App">
      <ThreeScene />
    </div>
  );
}

const rootElement = document.getElementById("root");
const root = createRoot(rootElement);
root.render(<App />);
