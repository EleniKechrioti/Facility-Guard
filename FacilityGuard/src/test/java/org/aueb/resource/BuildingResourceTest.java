package org.aueb.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.aueb.representation.AreaRepresentation;
import org.aueb.representation.BuildingRepresentation;
import org.aueb.representation.CheckpointRepresentation;
import org.aueb.util.Address;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class BuildingResourceTest {

    @Test
    void testCreateAndGetBuilding() {
        BuildingRepresentation buildingDto = new BuildingRepresentation();
        buildingDto.name = "Integration Test Building";
        buildingDto.address = new Address("Stournari", "55", "Athens", "10432", "Greece");

        // POST creation
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

        // GET confirmation
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

        AreaRepresentation areaDto = new AreaRepresentation();
        areaDto.name = "Secret Lab";

        // POST in sub-resource
        given()
                .contentType(ContentType.JSON)
                .body(areaDto)
                .when()
                .post("/buildings/" + buildingId + "/areas")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Secret Lab"))
                // We check if Mapper correctly put the building ID in the answer
                .body("buildingId", equalTo(buildingId));
    }

    /**
     * Checks if we can get the list of zones
     */
    @Test
    void testGetAreasOfBuilding() {
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
     * Checks if Validation is working
     */
    @Test
    void testValidationFailure() {
        BuildingRepresentation invalidDto = new BuildingRepresentation();
        invalidDto.name = ""; // Empty Name -> Must Pop
        invalidDto.address = new Address("A", "1", "B", "2", "C");

        given()
                .contentType(ContentType.JSON)
                .body(invalidDto)
                .when()
                .post("/buildings")
                .then()
                .statusCode(400); // Bad Request
    }

    @Test
    void testAddAndGetCheckpoint() {
        BuildingRepresentation buildingDto = new BuildingRepresentation();
        buildingDto.name = "Security HQ";
        buildingDto.address = new Address("Evelpidon", "22", "Athens", "11362", "Greece");

        int buildingId = given()
                .contentType(ContentType.JSON)
                .body(buildingDto)
                .when().post("/buildings").then().extract().path("id");

        AreaRepresentation areaDto = new AreaRepresentation();
        areaDto.name = "Main Entrance";

        int areaId = given()
                .contentType(ContentType.JSON)
                .body(areaDto)
                .when().post("/buildings/" + buildingId + "/areas")
                .then().extract().path("id");

        // Action: Add Checkpoint (POST)
        CheckpointRepresentation cpDto = new CheckpointRepresentation();
        cpDto.name = "Turnstile A";

        int checkpointId = given()
                .contentType(ContentType.JSON)
                .body(cpDto)
                .when()
                .post("/buildings/" + buildingId + "/areas/" + areaId + "/checkpoints")
                .then()
                .statusCode(201) // Created
                .body("checkpointId", notNullValue())
                .body("name", equalTo("Turnstile A"))
                .body("areaId", equalTo(areaId)) // We confirm that it was connected correctly
                .extract().path("checkpointId");

        // Action: Recover Checkpoints (GET)
        given()
                .when()
                .get("/buildings/" + buildingId + "/areas/" + areaId + "/checkpoints")
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].checkpointId", equalTo(checkpointId))
                .body("[0].name", equalTo("Turnstile A"));
    }

    @Test
    void testCheckpointHierarchyMismatch() {
        // Scenario: We try to put checkpoints in a zone,
        // but in the URL we give WRONG building.
        // It must fail because the zone does not belong to this building.

        BuildingRepresentation b1 = new BuildingRepresentation();
        b1.name = "Building Alpha";
        b1.address = new Address("A", "1", "A", "1", "A");
        int b1Id = given().contentType(ContentType.JSON).body(b1).when().post("/buildings").then().extract().path("id");

        BuildingRepresentation b2 = new BuildingRepresentation();
        b2.name = "Building Beta";
        b2.address = new Address("B", "2", "B", "2", "B");
        int b2Id = given().contentType(ContentType.JSON).body(b2).when().post("/buildings").then().extract().path("id");

        AreaRepresentation areaDto = new AreaRepresentation();
        areaDto.name = "Zone Alpha";
        int areaId = given().contentType(ContentType.JSON).body(areaDto)
                .when().post("/buildings/" + b1Id + "/areas").then().extract().path("id");

        // Action: POST attempt in Building B (but with Area A)
        CheckpointRepresentation cpDto = new CheckpointRepresentation();
        cpDto.name = "Hacker Gate";

        given()
                .contentType(ContentType.JSON)
                .body(cpDto)
                .when()
                .post("/buildings/" + b2Id + "/areas/" + areaId + "/checkpoints")
                .then()
                .statusCode(400); // Expect Bad Request (due to the control we put in Resource)
    }
}