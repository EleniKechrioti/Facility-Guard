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
        int buildingId = createBuilding("Multi-Area HQ");

        AreaRepresentation areaDto = new AreaRepresentation();
        areaDto.name = "Lobby";
        given().contentType(ContentType.JSON).body(areaDto)
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

        given().contentType(ContentType.JSON).body(invalidDto)
                .when().post("/buildings").then().statusCode(400);
    }

    @Test
    void testAddAndGetCheckpoint() {
        int buildingId = createBuilding("Security HQ");
        int areaId = createArea(buildingId, "Main Entrance");

        CheckpointRepresentation cpDto = new CheckpointRepresentation();
        cpDto.name = "Turnstile A";

        int checkpointId = given()
                .contentType(ContentType.JSON)
                .body(cpDto)
                .when()
                .post("/buildings/" + buildingId + "/areas/" + areaId + "/checkpoints")
                .then()
                .statusCode(201)
                .body("checkpointId", notNullValue())
                .extract().path("checkpointId");

        given()
                .when()
                .get("/buildings/" + buildingId + "/areas/" + areaId + "/checkpoints")
                .then()
                .statusCode(200)
                .body("[0].checkpointId", equalTo(checkpointId));
    }

    @Test
    void testCheckpointHierarchyMismatch() {
        int b1Id = createBuilding("Building Alpha");
        int b2Id = createBuilding("Building Beta");
        int areaId = createArea(b1Id, "Zone Alpha");

        CheckpointRepresentation cpDto = new CheckpointRepresentation();
        cpDto.name = "Hacker Gate";

        given()
                .contentType(ContentType.JSON)
                .body(cpDto)
                .when()
                // Trying to add to Area (which is in Alpha) via Beta URL
                .post("/buildings/" + b2Id + "/areas/" + areaId + "/checkpoints")
                .then()
                .statusCode(400);
    }


    @Test
    void testConnectAndDisconnectNeighbors() {
        int buildingId = createBuilding("Topology HQ");
        int area1 = createArea(buildingId, "Hallway");
        int area2 = createArea(buildingId, "Office");

        // Connect (PUT)
        given()
                .when()
                .put("/buildings/areas/" + area1 + "/neighbors/" + area2)
                .then()
                .statusCode(200)
                .body("status", equalTo("Connected"));

        // Disconnect (DELETE)
        given()
                .when()
                .delete("/buildings/areas/" + area1 + "/neighbors/" + area2)
                .then()
                .statusCode(204); // No Content
    }

    @Test
    void testDeleteBuilding() {
        int buildingId = createBuilding("Temporary Building");
        createArea(buildingId, "Temp Zone");

        // Delete (DELETE)
        given()
                .when()
                .delete("/buildings/" + buildingId)
                .then()
                .statusCode(204); // No Content

        // Verify it's gone
        AreaRepresentation checkArea = new AreaRepresentation();
        checkArea.name = "Check Deleted";

        given()
                .contentType(ContentType.JSON)
                .body(checkArea) // We send the valid item
                .when()
                .post("/buildings/" + buildingId + "/areas")
                .then()
                .statusCode(404);
    }

    // --- Helper Methods ---

    private int createBuilding(String name) {
        BuildingRepresentation b = new BuildingRepresentation();
        b.name = name;
        b.address = new Address("Test", "1", "Test", "11111", "Test");
        return given().contentType(ContentType.JSON).body(b)
                .when().post("/buildings").then().extract().path("id");
    }

    private int createArea(int buildingId, String name) {
        AreaRepresentation a = new AreaRepresentation();
        a.name = name;
        return given().contentType(ContentType.JSON).body(a)
                .when().post("/buildings/" + buildingId + "/areas").then().extract().path("id");
    }
}