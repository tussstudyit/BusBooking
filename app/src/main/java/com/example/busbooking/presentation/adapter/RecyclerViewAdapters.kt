package com.example.busbooking.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.busbooking.R
import com.example.busbooking.data.entity.Seat
import com.example.busbooking.data.relations.TripWithRouteAndBus
import com.example.busbooking.data.relations.TicketDetails
import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView

// ════════════════════════════════════════════════════════════════════════════════
// ADAPTER 1: TripSearchAdapter
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Adapter for displaying trip search results in a RecyclerView.
 *
 * Data: List<TripWithRouteAndBus>
 * Layout: item_trip_search.xml
 * Lifecycle: Fragment observes ViewModel.uiState, sets trips via setTrips()
 */
interface ITripSearchAdapter {
    fun setTrips(trips: List<TripWithRouteAndBus>)
    fun setOnTripClickListener(listener: (tripId: Long) -> Unit)
}

class TripSearchAdapter : RecyclerView.Adapter<TripSearchAdapter.ViewHolder>(), ITripSearchAdapter {
    private var trips = emptyList<TripWithRouteAndBus>()
    private var onTripClick: ((Long) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_search, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    override fun getItemCount() = trips.size

    override fun setTrips(newTrips: List<TripWithRouteAndBus>) {
        trips = newTrips
        notifyDataSetChanged()
        // TODO: Use DiffUtil for better performance on large lists
    }

    override fun setOnTripClickListener(listener: (Long) -> Unit) {
        onTripClick = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textRoute: TextView = itemView.findViewById(R.id.text_route)
        private val textDepartureTime: TextView = itemView.findViewById(R.id.text_departure_time)
        private val textArrivalTime: TextView = itemView.findViewById(R.id.text_arrival_time)
        private val textPrice: TextView = itemView.findViewById(R.id.text_price)
        private val textAvailableSeats: TextView = itemView.findViewById(R.id.text_available_seats)

        fun bind(trip: TripWithRouteAndBus) {
            textRoute.text = "${trip.route.origin} → ${trip.route.destination}"
            textDepartureTime.text = trip.trip.departureTime
            textArrivalTime.text = trip.trip.arrivalTime
            textPrice.text = "₹${trip.trip.price}"
            // TODO: Get actual available seats count from DAO
            textAvailableSeats.text = "Available seats: 15"

            itemView.setOnClickListener {
                onTripClick?.invoke(trip.trip.id)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// ADAPTER 2: SeatGridAdapter ⭐ CRITICAL
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Adapter for displaying seats in a visual grid (GridLayoutManager).
 *
 * Features:
 *   - 4 seat states (Available, Selected, Booked, Blocked)
 *   - Single selection (deselect previous, select new)
 *   - Only AVAILABLE seats clickable
 *   - Visual feedback (colors, icons, borders)
 *
 * Data: List<Seat> (already filtered to free seats by DAO)
 * Layout: item_seat.xml
 * GridLayoutManager: 6 columns
 */

enum class SeatStatus {
    AVAILABLE,      // Light gray, clickable
    SELECTED,       // Green with checkmark
    BOOKED,         // Dark gray/red with X
    BLOCKED         // Striped pattern with warning
}

interface ISeatGridAdapter {
    fun setSeats(seats: List<Seat>)
    fun selectSeat(seatId: Long)
    fun getSelectedSeatId(): Long?
    fun setOnSeatClickListener(listener: (seat: Seat) -> Unit)
}

class SeatGridAdapter : RecyclerView.Adapter<SeatGridAdapter.ViewHolder>(), ISeatGridAdapter {
    private var seats = emptyList<Seat>()
    private var selectedSeatId: Long? = null
    private var onSeatClick: ((Seat) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seat, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(seats[position])
    }

    override fun getItemCount() = seats.size

    override fun setSeats(newSeats: List<Seat>) {
        seats = newSeats
        notifyDataSetChanged()
    }

    override fun selectSeat(seatId: Long) {
        val oldSelected = selectedSeatId
        selectedSeatId = seatId

        // Notify old selected item to repaint (if exists)
        if (oldSelected != null) {
            val oldIndex = seats.indexOfFirst { it.id == oldSelected }
            if (oldIndex != -1) {
                notifyItemChanged(oldIndex)
            }
        }

        // Notify new selected item to repaint
        val newIndex = seats.indexOfFirst { it.id == seatId }
        if (newIndex != -1) {
            notifyItemChanged(newIndex)
        }
    }

    override fun getSelectedSeatId(): Long? = selectedSeatId

    override fun setOnSeatClickListener(listener: (Seat) -> Unit) {
        onSeatClick = listener
    }

    private fun getSeatStatus(seat: Seat): SeatStatus {
        return when {
            seat.id == selectedSeatId -> SeatStatus.SELECTED
            seat.status == "BLOCKED" -> SeatStatus.BLOCKED
            seat.status == "BOOKED" -> SeatStatus.BOOKED
            else -> SeatStatus.AVAILABLE
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: ViewGroup = itemView.findViewById(R.id.seat_item_container)
        private val seatNumber: TextView = itemView.findViewById(R.id.seat_number)
        private val seatIcon: ImageView = itemView.findViewById(R.id.seat_icon)

        fun bind(seat: Seat) {
            val status = getSeatStatus(seat)
            seatNumber.text = seat.seatNumber

            when (status) {
                SeatStatus.AVAILABLE -> {
                    container.setBackgroundColor(Color.parseColor("#CCCCCC"))
                    seatIcon.visibility = View.GONE
                    itemView.isClickable = true
                    itemView.isEnabled = true
                    itemView.alpha = 1.0f
                }

                SeatStatus.SELECTED -> {
                    container.setBackgroundColor(Color.parseColor("#00CC00"))
                    seatNumber.setTextColor(Color.WHITE)
                    seatIcon.visibility = View.VISIBLE
                    seatIcon.setImageResource(android.R.drawable.ic_menu_check)
                    itemView.isClickable = false
                    itemView.isEnabled = false
                }

                SeatStatus.BOOKED -> {
                    container.setBackgroundColor(Color.parseColor("#CC0000"))
                    seatNumber.setTextColor(Color.WHITE)
                    seatIcon.visibility = View.VISIBLE
                    seatIcon.setImageResource(android.R.drawable.ic_menu_delete)
                    itemView.isClickable = false
                    itemView.isEnabled = false
                    itemView.alpha = 0.6f
                }

                SeatStatus.BLOCKED -> {
                    container.setBackgroundColor(Color.parseColor("#FFFFFF"))
                    // TODO: Set striped pattern drawable
                    seatIcon.visibility = View.VISIBLE
                    seatIcon.setImageResource(android.R.drawable.ic_menu_info_details)
                    itemView.isClickable = false
                    itemView.isEnabled = false
                    itemView.alpha = 0.5f
                }
            }

            // Click listener only for AVAILABLE seats
            itemView.setOnClickListener {
                if (status == SeatStatus.AVAILABLE) {
                    onSeatClick?.invoke(seat)
                    selectSeat(seat.id)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// ADAPTER 3: TicketListAdapter
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Adapter for displaying user's bookings (active + history).
 *
 * Data: List<TicketDetails>
 * Layout: item_ticket.xml
 * Features: Status badge color coding, click to view details
 */
interface ITicketListAdapter {
    fun setTickets(tickets: List<TicketDetails>)
    fun setOnTicketClickListener(listener: (ticketId: Long) -> Unit)
}

class TicketListAdapter : RecyclerView.Adapter<TicketListAdapter.ViewHolder>(), ITicketListAdapter {
    private var tickets = emptyList<TicketDetails>()
    private var onTicketClick: ((Long) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ticket, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(tickets[position])
    }

    override fun getItemCount() = tickets.size

    override fun setTickets(newTickets: List<TicketDetails>) {
        tickets = newTickets
        notifyDataSetChanged()
    }

    override fun setOnTicketClickListener(listener: (Long) -> Unit) {
        onTicketClick = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textDestination: TextView = itemView.findViewById(R.id.text_destination)
        private val textDate: TextView = itemView.findViewById(R.id.text_date)
        private val textSeat: TextView = itemView.findViewById(R.id.text_seat)
        private val textStatus: TextView = itemView.findViewById(R.id.text_status)
        private val textPrice: TextView = itemView.findViewById(R.id.text_price)
        private val statusBadge: View = itemView.findViewById(R.id.status_badge)

        fun bind(ticket: TicketDetails) {
            textDestination.text = "${ticket.trip.route.origin} → ${ticket.trip.route.destination}"
            textDate.text = ticket.trip.trip.tripDate
            textSeat.text = "Seat ${ticket.seat.seatNumber}"
            textStatus.text = ticket.ticket.status
            textPrice.text = "₹${ticket.trip.trip.price}"

            // Color status badge
            val statusColor = when (ticket.ticket.status) {
                "CONFIRMED" -> Color.parseColor("#00CC00")  // Green
                "PENDING" -> Color.parseColor("#FFFF00")    // Yellow
                "CANCELLED" -> Color.parseColor("#CC0000")  // Red
                "USED" -> Color.parseColor("#0000FF")       // Blue
                else -> Color.GRAY
            }
            statusBadge.setBackgroundColor(statusColor)

            itemView.setOnClickListener {
                onTicketClick?.invoke(ticket.ticket.id)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// ADAPTER 4: RouteManagementAdapter (Admin)
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Admin adapter for route CRUD operations.
 *
 * Data: List<Route>
 * Features: Edit & Delete buttons with confirmation dialog
 */
interface IRouteManagementAdapter {
    fun setRoutes(routes: List<Route>)
    fun setOnEditClickListener(listener: (routeId: Long) -> Unit)
    fun setOnDeleteClickListener(listener: (routeId: Long) -> Unit)
}

class RouteManagementAdapter : RecyclerView.Adapter<RouteManagementAdapter.ViewHolder>(),
    IRouteManagementAdapter {
    private var routes = emptyList<Route>()
    private var onEditClick: ((Long) -> Unit)? = null
    private var onDeleteClick: ((Long) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_admin, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(routes[position])
    }

    override fun getItemCount() = routes.size

    override fun setRoutes(newRoutes: List<Route>) {
        routes = newRoutes
        notifyDataSetChanged()
    }

    override fun setOnEditClickListener(listener: (Long) -> Unit) {
        onEditClick = listener
    }

    override fun setOnDeleteClickListener(listener: (Long) -> Unit) {
        onDeleteClick = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textRoute: TextView = itemView.findViewById(R.id.text_route)
        private val textDistance: TextView = itemView.findViewById(R.id.text_distance)
        private val textStatus: TextView = itemView.findViewById(R.id.text_status)
        private val buttonEdit: View = itemView.findViewById(R.id.button_edit)
        private val buttonDelete: View = itemView.findViewById(R.id.button_delete)

        fun bind(route: Route) {
            textRoute.text = "${route.origin} → ${route.destination}"
            textDistance.text = "${route.distance} km"
            textStatus.text = if (route.isActive) "Active" else "Inactive"

            buttonEdit.setOnClickListener {
                onEditClick?.invoke(route.id)
            }

            buttonDelete.setOnClickListener {
                showDeleteConfirmation(route.id, "${route.origin} → ${route.destination}")
            }
        }

        private fun showDeleteConfirmation(routeId: Long, routeName: String) {
            // TODO: Show AlertDialog.Builder confirmation
            // On confirm: onDeleteClick?.invoke(routeId)
        }
    }
}

/**
 * TODO: Add DTO import
 * import com.example.busbooking.data.entity.Route
 */

