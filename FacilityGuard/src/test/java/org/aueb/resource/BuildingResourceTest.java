package org.aueb.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.aueb.representation.AreaRepresentation;
import org.aueb.representation.BuildingRepresentation;
import org.aueb.util.Address;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class BuildingResourceTest {

    @Test
    void testCreateAndGetBuilding() {
        // 1. Ετοιμάζουμε το JSON
        BuildingRepresentation buildingDto = new BuildingRepresentation();
        buildingDto.name = "Integration Test Building";
        buildingDto.address = new Address("Stournari", "55", "Athens", "10432", "Greece");

        // 2. POST (Δημιουργία)
        int buildingId = given()
                .contentType(ContentType.JSON)
                .body(buildingDto)
                .when()
                .post("/buildings")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Integration Test Building"))
                .extract().path("id");

        // 3. GET (Επιβεβαίωση)
        given()
                .when()
                .get("/buildings")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("find { it.id == " + buildingId + " }.name", equalTo("Integration Test Building"));
    }

    @Test
    void testAddAreaToBuilding() {
        // 1. Δημιουργούμε πρώτα ένα κτίριο
        BuildingRepresentation buildingDto = new BuildingRepresentation();
        buildingDto.name = "Area Test HQ";
        buildingDto.address = new Address("Patission", "76", "Athens", "10434", "Greece");

        int buildingId = given()
                .contentType(ContentType.JSON)
                .body(buildingDto)
                .when()
                .post("/buildings")
                .then()
                .statusCode(201)
                .extract().path("id");

        // 2. Ετοιμάζουμε το Area JSON
        // (Δεν βάζουμε description γιατί το αφαιρέσαμε, ούτε buildingId γιατί μπαίνει αυτόματα)
        AreaRepresentation areaDto = new AreaRepresentation();
        areaDto.name = "Secret Lab";

        // 3. POST στο sub-resource
        given()
                .contentType(ContentType.JSON)
                .body(areaDto)
                .when()
                .post("/buildings/" + buildingId + "/areas")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Secret Lab"))
                // Ελέγχουμε αν το Mapper έβαλε σωστά το ID του κτιρίου στην απάντηση
                .body("buildingId", equalTo(buildingId));
    }

    /**
     * ΝΕΟ TEST: Ελέγχει αν μπορούμε να πάρουμε τη λίστα των ζωνών
     */
    @Test
    void testGetAreasOfBuilding() {
        // Setup: Κτίριο + Ζώνη
        BuildingRepresentation buildingDto = new BuildingRepresentation();
        buildingDto.name = "Multi-Area HQ";
        buildingDto.address = new Address("Patission", "80", "Athens", "10434", "Greece");

        int buildingId = given()
                .contentType(ContentType.JSON)
                .body(buildingDto)
                .when().post("/buildings").then().extract().path("id");

        AreaRepresentation areaDto = new AreaRepresentation();
        areaDto.name = "Lobby";

        given()
                .contentType(ContentType.JSON)
                .body(areaDto)
                .when().post("/buildings/" + buildingId + "/areas").then().statusCode(201);

        // Action: GET /buildings/{id}/areas
        given()
                .when()
                .get("/buildings/" + buildingId + "/areas")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].name", equalTo("Lobby"));
    }

    /**
     * ΝΕΟ TEST: Ελέγχει αν το Validation δουλεύει (π.χ. κενό όνομα)
     */
    @Test
    void testValidationFailure() {
        BuildingRepresentation invalidDto = new BuildingRepresentation();
        invalidDto.name = ""; // Κενό όνομα -> Πρέπει να σκάσει
        invalidDto.address = new Address("A", "1", "B", "2", "C");

        given()
                .contentType(ContentType.JSON)
                .body(invalidDto)
                .when()
                .post("/buildings")
                .then()
                .statusCode(400); // Bad Request
    }
}