const BASE_URL = 'http://localhost:9090';

// Simulated GPS waypoints: Mumbai → Pune → Bengaluru (truncated route)
const WAYPOINTS = [
    { lat: 18.9220, lng: 72.8347, label: 'Mumbai departure' },
    { lat: 18.5204, lng: 73.8567, label: 'Pune' },
    { lat: 17.3850, lng: 76.8205, label: 'Solapur' },
    { lat: 16.8670, lng: 76.8945, label: 'Gulbarga' },
    { lat: 16.2076, lng: 77.3463, label: 'Raichur' },
    { lat: 15.3647, lng: 75.1240, label: 'Dharwad' },
    { lat: 14.4666, lng: 75.9238, label: 'Davangere' },
    { lat: 14.2251, lng: 76.3980, label: 'Tumkur' },
    { lat: 13.0827, lng: 80.2707, label: 'Chennai bypass' },
    { lat: 12.9716, lng: 77.5946, label: 'Bengaluru arrival' },
];

const PING_INTERVAL_MS = 3000; // 3 seconds between pings

async function post(path, token, body = null) {
    const opts = {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(`${BASE_URL}${path}`, opts);
    const json = await res.json();

    if (!res.ok) {
        throw new Error(`POST ${path} → ${res.status}: ${JSON.stringify(json)}`);
    }
    return json;
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function main() {
    const [,, shipmentId, token] = process.argv;

    if (!shipmentId || !token) {
        console.error('Usage: node simulate-driver.js <shipmentId> <carrierToken>');
        process.exit(1);
    }

    console.log(`\n🚚 Simulated Driver — Shipment #${shipmentId}`);
    console.log('='.repeat(50));

    try {
        // Step 1: Confirm pickup → AWAITING_PICKUP → IN_TRANSIT
        console.log('\n📦 Confirming pickup...');
        const pickupRes = await post(`/api/tracking/${shipmentId}/pickup`, token);
        console.log(`✅ ${pickupRes.message}`);
        console.log('   WebSocket broadcast: status=IN_TRANSIT sent to /topic/shipment/' + shipmentId);

        await sleep(1000);

        // Step 2: Send GPS pings
        console.log(`\n📡 Sending ${WAYPOINTS.length} GPS pings every ${PING_INTERVAL_MS / 1000}s...\n`);

        for (let i = 0; i < WAYPOINTS.length; i++) {
            const wp = WAYPOINTS[i];
            const res = await post(`/api/tracking/${shipmentId}/location`, token, {
                latitude: wp.lat,
                longitude: wp.lng,
            });

            console.log(`[${String(i + 1).padStart(2, '0')}/${WAYPOINTS.length}] 📍 ${wp.label}`);
            console.log(`       lat=${res.latitude}, lng=${res.longitude}`);
            console.log(`       → WebSocket broadcast to /topic/shipment/${shipmentId}`);

            if (i < WAYPOINTS.length - 1) {
                await sleep(PING_INTERVAL_MS);
            }
        }

        await sleep(1000);

        // Step 3: Confirm delivery → IN_TRANSIT → DELIVERED
        console.log('\n🏁 Confirming delivery...');
        const deliveryRes = await post(`/api/tracking/${shipmentId}/delivery`, token);
        console.log(`✅ ${deliveryRes.message}`);
        console.log('   WebSocket broadcast: status=DELIVERED sent to /topic/shipment/' + shipmentId);

        console.log('\n' + '='.repeat(50));
        console.log(`🎉 Simulation complete for Shipment #${shipmentId}`);
        console.log('   Check the React dashboard — delivery confirmed on map.');

    } catch (err) {
        console.error('\n❌ Simulation error:', err.message);
        process.exit(1);
    }
}

main();