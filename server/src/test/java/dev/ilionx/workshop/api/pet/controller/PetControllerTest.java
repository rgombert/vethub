package dev.ilionx.workshop.api.pet.controller;

import dev.ilionx.workshop.api.owner.model.Owner;
import dev.ilionx.workshop.api.pet.model.Pet;
import dev.ilionx.workshop.api.pet.model.request.CreatePetRequest;
import dev.ilionx.workshop.api.pet.model.request.UpdatePetRequest;
import dev.ilionx.workshop.api.pet.model.response.PetResponse;
import dev.ilionx.workshop.support.IntegrationTest;
import io.github.jframe.exception.resource.ErrorResponseResource;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static dev.ilionx.workshop.api.Paths.*;
import static dev.ilionx.workshop.common.exception.ApiErrorCode.OWNER_NOT_FOUND;
import static dev.ilionx.workshop.common.exception.ApiErrorCode.PET_NOT_FOUND;
import static io.github.jframe.util.mapper.ObjectMappers.fromJson;
import static io.github.jframe.util.mapper.ObjectMappers.toJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integration Test - Pet Controller")
class PetControllerTest extends IntegrationTest {

    private static final int NON_EXISTENT_OWNER_ID = 999;
    private static final int NON_EXISTENT_PET_ID = 999;

    private static final String PET_NAME = "Leo";
    private static final LocalDate PET_BIRTH_DATE = LocalDate.of(2020, 9, 7);
    private static final String UPDATED_PET_NAME = "Max";

    // ========================= LIST =========================
    @Test
    @DisplayName("Should return pets when owner has pets")
    void shouldReturnPetsWhenOwnerHasPets() throws Exception {
        // Given: An owner with one pet exists in the database
        final Owner savedOwner = aSavedOwner();
        final Pet savedPet = aSavedPet(savedOwner);

        // When: Getting all pets for the owner
        // Then: The pet should be returned with correct structure including type and empty visits
        mockMvc.perform(get(OWNER_PETS, savedOwner.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(equalTo(savedPet.getId()))))
            .andExpect(jsonPath("$[0].name", is(equalTo(PET_NAME))))
            .andExpect(jsonPath("$[0].birthDate", is(equalTo(PET_BIRTH_DATE.toString()))))
            .andExpect(jsonPath("$[0].type.id", is(equalTo(1))))
            .andExpect(jsonPath("$[0].type.name", is(equalTo("Cat"))))
            .andExpect(jsonPath("$[0].ownerId", is(equalTo(savedOwner.getId()))))
            .andExpect(jsonPath("$[0].visits", is(empty())));
    }

    @Test
    @DisplayName("Should return empty list when owner has no pets")
    void shouldReturnEmptyListWhenOwnerHasNoPets() throws Exception {
        // Given: An owner exists with no pets
        final Owner savedOwner = aSavedOwner();

        // When: Getting all pets for the owner
        // Then: Empty array should be returned
        mockMvc.perform(get(OWNER_PETS, savedOwner.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", is(empty())));
    }

    @Test
    @DisplayName("Should return not found when owner does not exist for list pets")
    void shouldReturnNotFoundWhenOwnerDoesNotExistForListPets() throws Exception {
        // Given: No owner exists with ID 999

        // When: Getting pets for non-existent owner
        final ResultActions response = mockMvc.perform(get(OWNER_PETS, NON_EXISTENT_OWNER_ID));

        // Then: HTTP 404 Not Found should be returned with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(OWNER_NOT_FOUND.getReason())));
    }

    // ========================= GET BY ID =========================
    @Test
    @DisplayName("Should return pet when valid pet ID exists")
    void shouldReturnPetWhenValidPetIdExists() throws Exception {
        // Given: An owner with one pet exists in the database
        final Owner savedOwner = aSavedOwner();
        final Pet savedPet = aSavedPet(savedOwner);

        // When: Getting pet by ID
        // Then: Pet should be returned with all fields
        mockMvc.perform(get(OWNER_PET_BY_ID, savedOwner.getId(), savedPet.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(equalTo(savedPet.getId()))))
            .andExpect(jsonPath("$.name", is(equalTo(PET_NAME))))
            .andExpect(jsonPath("$.birthDate", is(equalTo(PET_BIRTH_DATE.toString()))))
            .andExpect(jsonPath("$.type.id", is(equalTo(1))))
            .andExpect(jsonPath("$.type.name", is(equalTo("Cat"))))
            .andExpect(jsonPath("$.ownerId", is(equalTo(savedOwner.getId()))))
            .andExpect(jsonPath("$.owner.id", is(equalTo(savedOwner.getId()))))
            .andExpect(jsonPath("$.owner.firstName", is(equalTo(savedOwner.getFirstName()))))
            .andExpect(jsonPath("$.owner.lastName", is(equalTo(savedOwner.getLastName()))))
            .andExpect(jsonPath("$.visits", is(empty())));
    }

    @Test
    @DisplayName("Should return not found when pet ID does not exist")
    void shouldReturnNotFoundWhenPetIdDoesNotExist() throws Exception {
        // Given: An owner exists but pet does not exist
        final Owner savedOwner = aSavedOwner();

        // When: Getting pet by non-existent ID
        final ResultActions response = mockMvc.perform(get(OWNER_PET_BY_ID, savedOwner.getId(), NON_EXISTENT_PET_ID));

        // Then: HTTP 404 Not Found should be returned with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(PET_NOT_FOUND.getReason())));
    }

    @Test
    @DisplayName("Should return not found when pet does not belong to owner for get by ID")
    void shouldReturnNotFoundWhenPetDoesNotBelongToOwnerForGetById() throws Exception {
        // Given: Owner1 with a pet exists, and a second owner without pets exists
        final Owner owner1 = aSavedOwner();
        final Pet pet = aSavedPet(owner1);
        final Owner owner2 = new Owner();
        owner2.setFirstName("Betty");
        owner2.setLastName("Davis");
        owner2.setAddress("638 Cardinal Ave.");
        owner2.setCity("Sun Prairie");
        owner2.setTelephone("6085551749");
        final Owner savedOwner2 = ownerRepository.save(owner2);

        // When: Getting pet by ID using owner2's ID (pet belongs to owner1)
        final ResultActions response = mockMvc.perform(get(OWNER_PET_BY_ID, savedOwner2.getId(), pet.getId()));

        // Then: HTTP 404 Not Found should be returned with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(PET_NOT_FOUND.getReason())));
    }

    // ========================= CREATE =========================
    @Test
    @DisplayName("Should create pet when valid data provided")
    void shouldCreatePetWhenValidDataProvided() throws Exception {
        // Given: An owner exists and a valid create pet request
        final Owner savedOwner = aSavedOwner();
        final CreatePetRequest request = aCreatePetRequest();

        // When: Creating the pet via POST
        // Then: HTTP 201 Created should be returned with pet response
        mockMvc.perform(
            post(OWNER_PETS, savedOwner.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(notNullValue())))
            .andExpect(jsonPath("$.name", is(equalTo(PET_NAME))))
            .andExpect(jsonPath("$.birthDate", is(equalTo(PET_BIRTH_DATE.toString()))))
            .andExpect(jsonPath("$.type.id", is(equalTo(1))))
            .andExpect(jsonPath("$.type.name", is(equalTo("Cat"))))
            .andExpect(jsonPath("$.ownerId", is(equalTo(savedOwner.getId()))))
            .andExpect(jsonPath("$.visits", is(empty())));
    }

    @Test
    @DisplayName("Should return not found when owner does not exist for create pet")
    void shouldReturnNotFoundWhenOwnerDoesNotExistForCreatePet() throws Exception {
        // Given: No owner exists with ID 999
        final CreatePetRequest request = aCreatePetRequest();

        // When: Creating pet for non-existent owner via POST
        final ResultActions response = mockMvc.perform(
            post(OWNER_PETS, NON_EXISTENT_OWNER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        );

        // Then: HTTP 404 Not Found should be returned with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(OWNER_NOT_FOUND.getReason())));
    }

    @Test
    @DisplayName("Should return pet in owner response after creation")
    void shouldReturnPetInOwnerResponseAfterCreation() throws Exception {
        // Given: An owner exists
        final Owner savedOwner = aSavedOwner();
        final CreatePetRequest createPetRequest = aCreatePetRequest();

        // When: Creating a pet via POST
        final String createResponse = mockMvc.perform(
            post(OWNER_PETS, savedOwner.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(createPetRequest))
        )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        final PetResponse createdPet = fromJson(createResponse, PetResponse.class);

        // Then: Getting the owner should return the owner with 1 pet in pets list
        mockMvc.perform(get(OWNER_BY_ID, savedOwner.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(equalTo(savedOwner.getId()))))
            .andExpect(jsonPath("$.pets", hasSize(1)))
            .andExpect(jsonPath("$.pets[0].id", is(equalTo(createdPet.getId()))))
            .andExpect(jsonPath("$.pets[0].name", is(equalTo(PET_NAME))))
            .andExpect(jsonPath("$.pets[0].birthDate", is(equalTo(PET_BIRTH_DATE.toString()))));
    }

    // ========================= UPDATE =========================
    @Test
    @DisplayName("Should update pet when valid data provided")
    void shouldUpdatePetWhenValidDataProvided() throws Exception {
        // Given: An owner with one pet exists and an update request with new name
        final Owner savedOwner = aSavedOwner();
        final Pet savedPet = aSavedPet(savedOwner);
        final UpdatePetRequest request = anUpdatePetRequest();

        // When: Updating the pet via PUT
        // Then: HTTP 200 OK should be returned with updated pet response
        mockMvc.perform(
            put(OWNER_PET_BY_ID, savedOwner.getId(), savedPet.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(notNullValue())))
            .andExpect(jsonPath("$.name", is(equalTo(UPDATED_PET_NAME))));
    }

    @Test
    @DisplayName("Should return not found when pet does not exist for update")
    void shouldReturnNotFoundWhenPetDoesNotExistForUpdate() throws Exception {
        // Given: An owner exists but pet does not exist
        final Owner savedOwner = aSavedOwner();
        final UpdatePetRequest request = anUpdatePetRequest();

        // When: Updating non-existent pet via PUT
        final ResultActions response = mockMvc.perform(
            put(OWNER_PET_BY_ID, savedOwner.getId(), NON_EXISTENT_PET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        );

        // Then: HTTP 404 Not Found should be returned with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(PET_NOT_FOUND.getReason())));
    }

    @Test
    @DisplayName("Should return not found when pet does not belong to owner for update")
    void shouldReturnNotFoundWhenPetDoesNotBelongToOwnerForUpdate() throws Exception {
        // Given: Owner1 with a pet exists, and a second owner without pets exists
        final Owner owner1 = aSavedOwner();
        final Pet pet = aSavedPet(owner1);
        final Owner owner2 = new Owner();
        owner2.setFirstName("Betty");
        owner2.setLastName("Davis");
        owner2.setAddress("638 Cardinal Ave.");
        owner2.setCity("Sun Prairie");
        owner2.setTelephone("6085551749");
        final Owner savedOwner2 = ownerRepository.save(owner2);
        final UpdatePetRequest request = anUpdatePetRequest();

        // When: Updating pet using owner2's ID (pet belongs to owner1) via PUT
        final ResultActions response = mockMvc.perform(
            put(OWNER_PET_BY_ID, savedOwner2.getId(), pet.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        );

        // Then: HTTP 404 Not Found should be returned with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(PET_NOT_FOUND.getReason())));
    }

    // ========================= DELETE =========================
    @Test
    @DisplayName("Should delete pet when pet exists")
    void shouldDeletePetWhenPetExists() throws Exception {
        // Given: An owner with one pet exists in the database
        final Owner savedOwner = aSavedOwner();
        final Pet savedPet = aSavedPet(savedOwner);

        // When: Deleting the pet
        // Then: Should return 204 no content
        mockMvc.perform(delete(OWNER_PET_BY_ID, savedOwner.getId(), savedPet.getId()))
            .andExpect(status().isNoContent());

        // And: The pet should no longer exist
        mockMvc.perform(get(OWNER_PET_BY_ID, savedOwner.getId(), savedPet.getId()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return not found when pet does not exist for delete")
    void shouldReturnNotFoundWhenPetDoesNotExistForDelete() throws Exception {
        // Given: An owner exists but pet does not exist
        final Owner savedOwner = aSavedOwner();

        // When: Deleting non-existent pet
        final ResultActions response = mockMvc.perform(delete(OWNER_PET_BY_ID, savedOwner.getId(), NON_EXISTENT_PET_ID));

        // Then: HTTP 404 Not Found should be returned with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(PET_NOT_FOUND.getReason())));
    }

    @Test
    @DisplayName("Should return not found when pet does not belong to owner for delete")
    void shouldReturnNotFoundWhenPetDoesNotBelongToOwnerForDelete() throws Exception {
        // Given: Owner1 with a pet exists, and a second owner without pets exists
        final Owner owner1 = aSavedOwner();
        final Pet pet = aSavedPet(owner1);
        final Owner owner2 = new Owner();
        owner2.setFirstName("Betty");
        owner2.setLastName("Davis");
        owner2.setAddress("638 Cardinal Ave.");
        owner2.setCity("Sun Prairie");
        owner2.setTelephone("6085551749");
        final Owner savedOwner2 = ownerRepository.save(owner2);

        // When: Deleting pet using owner2's ID (pet belongs to owner1)
        final ResultActions response = mockMvc.perform(delete(OWNER_PET_BY_ID, savedOwner2.getId(), pet.getId()));

        // Then: HTTP 404 Not Found should be returned with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(PET_NOT_FOUND.getReason())));
    }
}
