import {
  BrowserRouter,
  NavLink,
  Route,
  Routes,
} from "react-router-dom";

import Dashboard from "./pages/Dashboard";
import Flights from "./pages/Flights";
import Bookings from "./pages/Bookings";

import "./index.css";

function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <header className="header">
          <div>
            <h1>✈ Airline Recovery Platform</h1>
            <p>
              Flight disruption and passenger recovery
              operations
            </p>
          </div>

          <nav className="nav">
            <NavLink to="/">
              Dashboard
            </NavLink>

            <NavLink to="/flights">
              Flights
            </NavLink>

            <NavLink to="/bookings">
              Bookings
            </NavLink>
          </nav>
        </header>

        <main className="content">
          <Routes>
            <Route
              path="/"
              element={<Dashboard />}
            />

            <Route
              path="/flights"
              element={<Flights />}
            />

            <Route
              path="/bookings"
              element={<Bookings />}
            />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;