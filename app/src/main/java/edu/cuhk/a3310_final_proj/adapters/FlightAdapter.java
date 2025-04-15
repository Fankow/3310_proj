package edu.cuhk.a3310_final_proj.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.models.Airport;
import edu.cuhk.a3310_final_proj.models.Flight;

public class FlightAdapter extends RecyclerView.Adapter<FlightAdapter.FlightViewHolder> {

    private static final String TAG = "FlightAdapter";
    private Context context;
    private List<Flight> flights;
    private OnFlightClickListener listener;

    private static final java.util.Map<String, String> AIRPORT_NAMES = new java.util.HashMap<>();

    static {
        AIRPORT_NAMES.put("HKG", "Hong Kong International Airport");
        AIRPORT_NAMES.put("NRT", "Narita International Airport");
        AIRPORT_NAMES.put("HND", "Tokyo Haneda Airport");
        AIRPORT_NAMES.put("ICN", "Incheon International Airport");
        AIRPORT_NAMES.put("SIN", "Singapore Changi Airport");
        AIRPORT_NAMES.put("BKK", "Suvarnabhumi Airport");
        AIRPORT_NAMES.put("KIX", "Kansai International Airport");
        AIRPORT_NAMES.put("PEK", "Beijing Capital International Airport");
        AIRPORT_NAMES.put("PVG", "Shanghai Pudong International Airport");
        AIRPORT_NAMES.put("TPE", "Taiwan Taoyuan International Airport");
    }

    public FlightAdapter(Context context, List<Flight> flights) {
        this.context = context;
        this.flights = flights;
    }

    @NonNull
    @Override
    public FlightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_flight, parent, false);
        return new FlightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlightViewHolder holder, int position) {
        Flight flight = flights.get(position);
        Log.d(TAG, "Flight #" + position + " details:");
        Log.d(TAG, "  Airline: " + flight.getAirline());
        Log.d(TAG, "  Flight Number: " + flight.getFlightNumber());
        Log.d(TAG, "  Flight Number is null: " + (flight.getFlightNumber() == null));
        Log.d(TAG, "  Flight Number is empty: " + (flight.getFlightNumber() != null && flight.getFlightNumber().isEmpty()));

        if (flight.getAirline() != null) {
            holder.tvAirlineName.setText(flight.getAirline());
        } else {
            holder.tvAirlineName.setText("Unknown Airline");
        }

        if (flight.getFlightNumber() != null) {
            holder.tvFlightNumber.setText("Flight " + flight.getFlightNumber());
        } else {
            holder.tvFlightNumber.setText("Flight number unavailable");
        }


        if (flight.getAirlineIcon() != null && !flight.getAirlineIcon().isEmpty()) {
            Glide.with(context)
                    .load(flight.getAirlineIcon())
                    .placeholder(R.drawable.ic_airplane)
                    .error(R.drawable.ic_airplane)
                    .into(holder.ivAirlineLogo);
        } else {
            holder.ivAirlineLogo.setImageResource(R.drawable.ic_airplane);
        }

        String departureCode = getAirportCode(flight, true);
        holder.tvDepartureAirportCode.setText(departureCode);

        String arrivalCode = getAirportCode(flight, false);
        holder.tvArrivalAirportCode.setText(arrivalCode);

        String departureTime = getFormattedTime(flight, true);
        String arrivalTime = getFormattedTime(flight, false);
        holder.tvDepartureTime.setText(departureTime.isEmpty() ? "---" : departureTime);
        holder.tvArrivalTime.setText(arrivalTime.isEmpty() ? "---" : arrivalTime);

        if (holder.tvDepartureAirportName != null) {
            holder.tvDepartureAirportName.setText(getAirportName(departureCode));
        }

        if (holder.tvArrivalAirportName != null) {
            holder.tvArrivalAirportName.setText(getAirportName(arrivalCode));
        }

        holder.tvDuration.setText(flight.getFormattedDuration());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFlightClick(flight);
            }
        });
    }

    private String getAirportCode(Flight flight, boolean isDeparture) {
        if (isDeparture) {
            Airport airport = flight.getDepartureAirport();
            if (airport != null && airport.getId() != null && !airport.getId().isEmpty()) {
                return airport.getId();
            }
        } else {
            Airport airport = flight.getArrivalAirport();
            if (airport != null && airport.getId() != null && !airport.getId().isEmpty()) {
                return airport.getId();
            }
        }
        return isDeparture ? "NRT" : "HKG";
    }

    private String getAirportName(String airportCode) {
        if (airportCode != null && AIRPORT_NAMES.containsKey(airportCode)) {
            return AIRPORT_NAMES.get(airportCode);
        }
        return "Unknown Airport";
    }

    private String getFormattedTime(Flight flight, boolean isDeparture) {
        String time = "";
        try {
            Log.d(TAG, "Flight object: " + flight);

            if (isDeparture) {
                Log.d(TAG, "Departure airport object: " + flight.getDepartureAirport());
            } else {
                Log.d(TAG, "Arrival airport object: " + flight.getArrivalAirport());
            }

            Airport airport = isDeparture ? flight.getDepartureAirport() : flight.getArrivalAirport();

            if (airport != null) {
                Log.d(TAG, "Airport object not null");
                Log.d(TAG, "Airport time: " + airport.getTime());

                if (airport.getTime() != null && !airport.getTime().isEmpty()) {
                    time = extractTimeFromDateTime(airport.getTime());
                    Log.d(TAG, "Extracted time from airport: " + time);
                } else {
                    Log.d(TAG, "Airport time is null or empty");
                }
            } else {
                Log.d(TAG, "Airport object is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting time: " + e.getMessage(), e);
        }

        return time.isEmpty() ? "---" : time;
    }

    private String extractTimeFromDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return "";
        }

        try {
            Log.d(TAG, "Parsing dateTime: " + dateTime);

            String[] parts;
            if (dateTime.contains(" ")) {
                parts = dateTime.split(" ");
                if (parts.length > 1) {
                    Log.d(TAG, "Extracted time from space-separated format: " + parts[1]);
                    return parts[1];
                }
            } else if (dateTime.contains("T")) {
                parts = dateTime.split("T");
                if (parts.length > 1) {
                    String timePart = parts[1];
                    if (timePart.contains(":")) {
                        String[] timeParts = timePart.split(":");
                        if (timeParts.length >= 2) {
                            String result = timeParts[0] + ":" + timeParts[1];
                            Log.d(TAG, "Extracted time from T-separated format: " + result);
                            return result;
                        }
                    }
                    Log.d(TAG, "Extracted time part (unprocessed): " + timePart);
                    return timePart;
                }
            }

            Log.d(TAG, "Could not parse dateTime using standard methods: " + dateTime);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing dateTime: " + dateTime + ", error: " + e.getMessage(), e);
        }

        return dateTime;
    }

    @Override
    public int getItemCount() {
        return flights.size();
    }

    public void setOnFlightClickListener(OnFlightClickListener listener) {
        this.listener = listener;
    }

    public interface OnFlightClickListener {

        void onFlightClick(Flight flight);
    }

    public class FlightViewHolder extends RecyclerView.ViewHolder {

        TextView tvAirlineName, tvFlightNumber;
        TextView tvDepartureTime, tvDepartureAirportCode, tvDepartureAirportName;
        TextView tvArrivalTime, tvArrivalAirportCode, tvArrivalAirportName;
        TextView tvDuration;
        ImageView ivAirlineLogo;

        public FlightViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAirlineLogo = itemView.findViewById(R.id.iv_airline_logo);
            tvAirlineName = itemView.findViewById(R.id.tv_airline_name);
            tvFlightNumber = itemView.findViewById(R.id.tv_flight_number);

            tvDepartureTime = itemView.findViewById(R.id.tv_departure_time);
            tvDepartureAirportCode = itemView.findViewById(R.id.tv_departure_airport_code);

            tvArrivalTime = itemView.findViewById(R.id.tv_arrival_time);
            tvArrivalAirportCode = itemView.findViewById(R.id.tv_arrival_airport_code);

            tvDuration = itemView.findViewById(R.id.tv_duration);
        }
    }
}
