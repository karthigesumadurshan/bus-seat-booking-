package com.example.busbooking;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int TOTAL_SEATS = 54;
    private static final int SEATS_PER_ROW = 5;   // 3 (driver side) + aisle + 2 (door side)

    // Real-bus layout:
    // 9 full rows of 3+2 (with aisle)              -> seats 1-45
    // 1 "door row": only 3 driver-side seats,
    //   door-side is empty because the back door is there -> seats 46-48
    // Back bench: remaining seats in one row, no aisle    -> seats 49-54
    private static final int FULL_ROWS = 9;             // 9 x 5 = 45 seats
    private static final int DOOR_ROW_SEATS = 3;        // driver-side only, 46-48

    private static final String PREFS_NAME = "bus_booking_prefs";
    private static final String KEY_BOOKINGS = "bookings_json";

    private final List<Seat> seatList = new ArrayList<>();
    private final Set<Integer> selectedSeatNumbers = new HashSet<>();
    private final Map<Integer, Button> seatButtons = new HashMap<>();

    private LinearLayout seatMapContainer;
    private LinearLayout backRowContainer;
    private TextView selectionSummary;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        seatMapContainer = findViewById(R.id.seatMapContainer);
        backRowContainer = findViewById(R.id.backRowContainer);
        selectionSummary = findViewById(R.id.selectionSummary);
        Button bookBtn = findViewById(R.id.bookSeatsBtn);
        Button viewBookingsBtn = findViewById(R.id.viewBookingsBtn);
        Button resetBtn = findViewById(R.id.resetBtn);

        // 1. Create 54 seats
        for (int i = 1; i <= TOTAL_SEATS; i++) {
            seatList.add(new Seat(i));
        }

        // 2. Restore any previously saved bookings
        loadBookings();

        // 3. Build the actual bus-shaped seat map (3+2 rows + door row + back bench)
        buildSeatMap();
        refreshAllSeatUI();

        bookBtn.setOnClickListener(v -> showPassengerNameDialog());
        viewBookingsBtn.setOnClickListener(v -> showAllBookings());
        resetBtn.setOnClickListener(v -> confirmResetAllSeats());

        updateSelectionSummary();
    }

    // ---------- Building the bus-shaped seat map ----------

    private void buildSeatMap() {
        int seatIndex = 0; // pointer into seatList (0-based), seat numbers are seatIndex+1

        // 9 full rows of 3 (driver side) + aisle + 2 (door side)
        for (int row = 0; row < FULL_ROWS; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLayout.setLayoutParams(rowParams);

            // Door side (2 seats)
            rowLayout.addView(createSeatButton(seatList.get(seatIndex++)));
            rowLayout.addView(createSeatButton(seatList.get(seatIndex++)));

            // Aisle
            View aisle = new View(this);
            aisle.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(48)));
            rowLayout.addView(aisle);

            // Driver side (3 seats)
            rowLayout.addView(createSeatButton(seatList.get(seatIndex++)));
            rowLayout.addView(createSeatButton(seatList.get(seatIndex++)));
            rowLayout.addView(createSeatButton(seatList.get(seatIndex++)));

            seatMapContainer.addView(rowLayout);
        }

        // Door row: door-side seats are replaced by the back door, so only
        // the 3 driver-side seats exist here (these become seats 46-48).
        LinearLayout doorRowLayout = new LinearLayout(this);
        doorRowLayout.setOrientation(LinearLayout.HORIZONTAL);
        doorRowLayout.setGravity(Gravity.CENTER);
        doorRowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Placeholder occupying the space where the 2 door-side seats would be
        TextView doorLabel = new TextView(this);
        doorLabel.setText("DOOR");
        doorLabel.setTextSize(10);
        doorLabel.setTextColor(Color.DKGRAY);
        doorLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams doorParams = new LinearLayout.LayoutParams(
                dp(46 * 2 + 8), dp(46));
        doorParams.setMargins(dp(4), dp(4), dp(4), dp(4));
        doorLabel.setLayoutParams(doorParams);
        doorRowLayout.addView(doorLabel);

        View doorAisle = new View(this);
        doorAisle.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(48)));
        doorRowLayout.addView(doorAisle);

        // Driver side (3 seats) -> seat numbers 46, 47, 48
        for (int i = 0; i < DOOR_ROW_SEATS; i++) {
            doorRowLayout.addView(createSeatButton(seatList.get(seatIndex++)));
        }

        seatMapContainer.addView(doorRowLayout);

        // Back bench: remaining seats sit in one continuous row, no aisle
        // (like the last row of a real bus) -> seats 49-54
        while (seatIndex < TOTAL_SEATS) {
            backRowContainer.addView(createSeatButton(seatList.get(seatIndex++)));
        }
    }

    private Button createSeatButton(Seat seat) {
        Button b = new Button(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(46), dp(46));
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        b.setLayoutParams(params);
        b.setText(String.valueOf(seat.getSeatNumber()));
        b.setTextSize(11);
        b.setTextColor(Color.WHITE);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setMinimumWidth(0);
        b.setMinimumHeight(0);
        b.setAllCaps(false);

        int seatNumber = seat.getSeatNumber();
        b.setOnClickListener(v -> onSeatClicked(seatNumber));

        seatButtons.put(seatNumber, b);
        return b;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    // ---------- Selection + UI refresh ----------

    private void onSeatClicked(int seatNumber) {
        Seat seat = seatList.get(seatNumber - 1);
        if (seat.isBooked()) {
            Toast.makeText(this, "Seat " + seatNumber + " is already booked ("
                    + seat.getPassengerName() + ")", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedSeatNumbers.contains(seatNumber)) {
            selectedSeatNumbers.remove(seatNumber);
        } else {
            selectedSeatNumbers.add(seatNumber);
        }
        refreshSeatUI(seatNumber);
        updateSelectionSummary();
    }

    private void refreshSeatUI(int seatNumber) {
        Button b = seatButtons.get(seatNumber);
        if (b == null) return;
        Seat seat = seatList.get(seatNumber - 1);

        if (seat.isBooked()) {
            b.setBackgroundResource(R.drawable.seat_booked_bg);
            b.setEnabled(false);
        } else if (selectedSeatNumbers.contains(seatNumber)) {
            b.setBackgroundResource(R.drawable.seat_selected_bg);
            b.setEnabled(true);
        } else {
            b.setBackgroundResource(R.drawable.seat_available_bg);
            b.setEnabled(true);
        }
    }

    private void refreshAllSeatUI() {
        for (int i = 1; i <= TOTAL_SEATS; i++) {
            refreshSeatUI(i);
        }
    }

    private void updateSelectionSummary() {
        if (selectedSeatNumbers.isEmpty()) {
            selectionSummary.setText("No seats selected. Tap seats to select (you can pick 5, 10, or more).");
        } else {
            List<Integer> sorted = new ArrayList<>(selectedSeatNumbers);
            java.util.Collections.sort(sorted);
            StringBuilder sb = new StringBuilder("Selected (" + sorted.size() + "): ");
            for (int i = 0; i < sorted.size(); i++) {
                sb.append(sorted.get(i));
                if (i != sorted.size() - 1) sb.append(", ");
            }
            selectionSummary.setText(sb.toString());
        }
    }

    // ---------- Booking ----------

    private void showPassengerNameDialog() {
        if (selectedSeatNumbers.isEmpty()) {
            Toast.makeText(this, "Please select at least one seat first.", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Passenger name");
        int padding = dp(16);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Book " + selectedSeatNumbers.size() + " seat(s)")
                .setMessage("Enter the passenger name for the selected seats:")
                .setView(input)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    bookSelectedSeats(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void bookSelectedSeats(String passengerName) {
        int count = 0;
        for (int seatNumber : selectedSeatNumbers) {
            Seat seat = seatList.get(seatNumber - 1);
            if (!seat.isBooked()) {
                seat.setBooked(true);
                seat.setPassengerName(passengerName);
                count++;
            }
        }
        selectedSeatNumbers.clear();
        saveBookings();
        refreshAllSeatUI();
        updateSelectionSummary();
        Toast.makeText(this, "Booked " + count + " seat(s) for " + passengerName, Toast.LENGTH_LONG).show();
    }

    private void showAllBookings() {

        Map<String, List<Integer>> grouped = new LinkedHashMap<>();

        for (Seat seat : seatList) {
            if (seat.isBooked()) {
                grouped.computeIfAbsent(seat.getPassengerName(),
                                k -> new ArrayList<>())
                        .add(seat.getSeatNumber());
            }
        }

        if (grouped.isEmpty()) {
            Toast.makeText(this, "No bookings yet.", Toast.LENGTH_SHORT).show();
            return;
        }


        String[] passengers = grouped.keySet().toArray(new String[0]);


        new AlertDialog.Builder(this)
                .setTitle("Select Passenger to Edit")
                .setItems(passengers, (dialog, which) -> {

                    String passenger = passengers[which];

                    editBooking(passenger);

                })
                .show();
    }

    private void editBooking(String passengerName) {

        List<Integer> passengerSeats = new ArrayList<>();

        for (Seat seat : seatList) {

            if (seat.isBooked() &&
                    seat.getPassengerName().equals(passengerName)) {

                passengerSeats.add(seat.getSeatNumber());
            }
        }


        StringBuilder message = new StringBuilder();

        message.append("Passenger: ")
                .append(passengerName)
                .append("\n\nSeats: ")
                .append(passengerSeats);


        new AlertDialog.Builder(this)
                .setTitle("Edit Booking")
                .setMessage(message.toString())

                .setPositiveButton("Change Seats", (dialog, which) -> {

                    // Remove old seats temporarily

                    for(int seatNo : passengerSeats){

                        Seat seat = seatList.get(seatNo - 1);

                        seat.setBooked(false);
                        seat.setPassengerName(null);
                    }


                    saveBookings();
                    refreshAllSeatUI();


                    Toast.makeText(this,
                            "Select new seats for "+passengerName,
                            Toast.LENGTH_LONG).show();


                    selectedSeatNumbers.clear();


                })

                .setNegativeButton("Cancel", null)

                .show();
    }
    private void confirmResetAllSeats() {
        new AlertDialog.Builder(this)
                .setTitle("Reset all seats?")
                .setMessage("This clears every booking. This cannot be undone.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    for (Seat seat : seatList) {
                        seat.setBooked(false);
                        seat.setPassengerName(null);
                    }
                    selectedSeatNumbers.clear();
                    saveBookings();
                    refreshAllSeatUI();
                    updateSelectionSummary();
                    Toast.makeText(this, "All seats reset.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------- Persistence (SharedPreferences + JSON, no Firebase needed) ----------

    private void saveBookings() {
        JSONArray array = new JSONArray();
        try {
            for (Seat seat : seatList) {
                if (seat.isBooked()) {
                    JSONObject obj = new JSONObject();
                    obj.put("seatNumber", seat.getSeatNumber());
                    obj.put("passengerName", seat.getPassengerName());
                    array.put(obj);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        prefs.edit().putString(KEY_BOOKINGS, array.toString()).apply();
    }

    private void loadBookings() {
        String json = prefs.getString(KEY_BOOKINGS, null);
        if (json == null) return;

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                int seatNumber = obj.getInt("seatNumber");
                String passengerName = obj.getString("passengerName");

                Seat seat = seatList.get(seatNumber - 1);
                seat.setBooked(true);
                seat.setPassengerName(passengerName);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}