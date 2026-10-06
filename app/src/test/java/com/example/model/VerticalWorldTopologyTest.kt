package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerticalWorldTopologyTest {

    @Test
    fun bidirectionalConnectorResolvesBothDirections() {
        val connector = VerticalConnector(
            id = "street_to_mezzanine",
            source = Point3D(4f, 8f, 1f),
            destination = Point3D(4f, 8f, 2f),
            type = VerticalConnectorType.LADDER
        )
        val topology = VerticalWorldTopology.of(connector)

        assertEquals(
            connector,
            topology.between(GridPos(4, 8, 1), GridPos(4, 8, 2))
        )
        assertEquals(
            connector,
            topology.between(GridPos(4, 8, 2), GridPos(4, 8, 1))
        )
    }

    @Test
    fun topologyRejectsSameEndpointAndSameZConnector() {
        val invalid = listOf(
            VerticalConnector(
                id = "same_point",
                source = Point3D(1f, 1f, 1f),
                destination = Point3D(1f, 1f, 1f),
                type = VerticalConnectorType.LADDER
            ),
            VerticalConnector(
                id = "same_layer",
                source = Point3D(2f, 2f, 1f),
                destination = Point3D(3f, 2f, 1f),
                type = VerticalConnectorType.BRIDGE
            )
        )

        val errors = VerticalWorldTopology(invalid).validate()

        assertEquals(2, errors.size)
        assertTrue(errors.any { it.contains("same_point") })
        assertTrue(errors.any { it.contains("same_layer") })
    }
}
