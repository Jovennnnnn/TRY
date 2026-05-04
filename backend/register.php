<?php
header("Content-Type: application/json");
require_once 'db_config.php';

$data = json_decode(file_get_contents("php://input"));

if (!$data) {
    echo json_encode(["success" => false, "message" => "No data received"]);
    exit;
}

if (!empty($data->username) && !empty($data->password) && !empty($data->role)) {
    try {
        $hashed_password = password_hash($data->password, PASSWORD_BCRYPT);
        $name = !empty($data->name) ? $data->name : $data->username;

        if ($data->role === 'resident') {
            // TINANGGAL ANG area_id SA QUERY PARA MAIWASAN ANG FOREIGN KEY ERROR (1452)
            $query = "INSERT INTO residents (username, name, password_hash, email, phone, purok, complete_address)
                      VALUES (?, ?, ?, ?, ?, ?, ?)";
            $stmt = $conn->prepare($query);

            $email = !empty($data->email) ? $data->email : "";
            $phone = !empty($data->phone) ? $data->phone : null;
            $purok = !empty($data->purok) ? $data->purok : "";
            $address = !empty($data->complete_address) ? $data->complete_address : "";

            $stmt->execute([$data->username, $name, $hashed_password, $email, $phone, $purok, $address]);
            echo json_encode(["success" => true, "message" => "Resident Registered Successfully"]);

        } else if ($data->role === 'admin' || $data->role === 'driver') {
            // REGISTRATION PARA SA USERS (Admin/Driver)
            $query = "INSERT INTO users (username, name, email, password_hash, phone, license_number, preferred_truck, role)
                      VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            $stmt = $conn->prepare($query);

            $email = !empty($data->email) ? $data->email : "";
            $phone = !empty($data->phone) ? $data->phone : null;
            $license = !empty($data->license_number) ? $data->license_number : "";
            $truck = !empty($data->preferred_truck) ? $data->preferred_truck : null;

            $stmt->execute([$data->username, $name, $email, $hashed_password, $phone, $license, $truck, $data->role]);
            echo json_encode(["success" => true, "message" => "User Registered Successfully"]);
        }

    } catch (PDOException $e) {
        echo json_encode(["success" => false, "message" => "Database Error: " . $e->getMessage()]);
    }
} else {
    echo json_encode(["success" => false, "message" => "Incomplete data"]);
}
?>