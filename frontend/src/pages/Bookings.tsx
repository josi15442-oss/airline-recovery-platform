import { useEffect, useState } from "react";
import api from "../services/api";
import type { Booking } from "../types/Booking";
import type { Flight } from "../types/Flight";

function Bookings() {
  const [flights, setFlights] = useState<Flight[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);

  const [selectedFlightId, setSelectedFlightId] = useState("");
  const [passengerId, setPassengerId] = useState("");

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    loadFlights();
  }, []);

  const loadFlights = async () => {
    try {
      const response = await api.get<Flight[]>("/flights");
      setFlights(response.data);
    } catch (err) {
      console.error(err);
      setError("Unable to load flights.");
    }
  };

  const loadBookings = async (flightId: string) => {
    if (!flightId) {
      setBookings([]);
      return;
    }

    try {
      const response = await api.get<Booking[]>(
        `/bookings/flight/${flightId}`
      );

      setBookings(response.data);
      setError("");
    } catch (err) {
      console.error(err);
      setError("Unable to load bookings.");
    }
  };

  const handleFlightChange = async (
    event: React.ChangeEvent<HTMLSelectElement>
  ) => {
    const flightId = event.target.value;

    setSelectedFlightId(flightId);

    await loadBookings(flightId);
  };

  const createBooking = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault();

    if (!passengerId || !selectedFlightId) {
      setError("Passenger ID and flight are required.");
      return;
    }

    try {
      await api.post("/bookings", {
        passengerId,
        flightId: Number(selectedFlightId),
      });

      setMessage("Booking created successfully.");
      setError("");
      setPassengerId("");

      await loadBookings(selectedFlightId);
      await loadFlights();
    } catch (err: any) {
      console.error(err);

      setMessage("");

      setError(
        err.response?.data?.message ||
          "Unable to create booking."
      );
    }
  };

  return (
    <div>
      <div className="page-title">
        <div>
          <h2>Bookings</h2>
          <p>Create reservations and monitor passenger recovery.</p>
        </div>
      </div>

      <div className="booking-layout">
        <div className="booking-card">
          <h3>Create Booking</h3>

          <form onSubmit={createBooking}>
            <label>Passenger ID</label>

            <input
              type="text"
              value={passengerId}
              placeholder="PAX-3001"
              onChange={(event) =>
                setPassengerId(event.target.value)
              }
            />

            <label>Flight</label>

            <select
              value={selectedFlightId}
              onChange={handleFlightChange}
            >
              <option value="">Select flight</option>

            {flights
  .filter(
    (flight) =>
      flight.status !== "CANCELLED" &&
      flight.status !== "DEPARTED" &&
      flight.status !== "ARRIVED" &&
      flight.availableSeats > 0
  )
  .map((flight) => (
    <option
      key={flight.id}
      value={flight.id}
    >
      {flight.flightNumber} — {flight.origin} →{" "}
      {flight.destination} — {flight.availableSeats} seats
    </option>
  ))}
            </select>

            <button
              className="primary-button"
              type="submit"
            >
              Create Booking
            </button>
          </form>

          {message && (
            <div className="success-message">
              {message}
            </div>
          )}

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}
        </div>

        <div className="booking-table-container">
          <h3>Passengers</h3>

          {!selectedFlightId ? (
            <p>Select a flight to view its passengers.</p>
          ) : bookings.length === 0 ? (
            <p>No passengers booked on this flight.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Passenger</th>
                  <th>Flight</th>
                  <th>Route</th>
                  <th>Status</th>
                  <th>Created</th>
                </tr>
              </thead>

              <tbody>
                {bookings.map((booking) => (
                  <tr key={booking.id}>
                    <td>
                      <strong>
                        {booking.passengerId}
                      </strong>
                    </td>

                    <td>
                      {booking.flight.flightNumber}
                    </td>

                    <td>
                      {booking.flight.origin} →{" "}
                      {booking.flight.destination}
                    </td>

                    <td>
                      <span
                        className={`status booking-status-${booking.status.toLowerCase()}`}
                      >
                        {booking.status}
                      </span>
                    </td>

                    <td>
                      {new Date(
                        booking.createdAt
                      ).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

export default Bookings;