export interface Flight {
  id: number;
  flightNumber: string;
  origin: string;
  destination: string;
  departureTime: string;
  arrivalTime: string;
  status:
    | "SCHEDULED"
    | "ON_TIME"
    | "DELAYED"
    | "CANCELLED"
    | "DEPARTED"
    | "ARRIVED";
  availableSeats: number;
}