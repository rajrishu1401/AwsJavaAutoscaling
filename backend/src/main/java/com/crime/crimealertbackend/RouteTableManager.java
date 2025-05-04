package com.crime.crimealertbackend;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;

public class RouteTableManager {
    private final Ec2Client ec2;

    public RouteTableManager() {
        this.ec2 = Ec2Client.create();
    }

    // Method to check if Route Table exists by name and VPC ID
    private String findRouteTableByName(String vpcId, String routeTableName) {
        DescribeRouteTablesResponse response = ec2.describeRouteTables();

        for (RouteTable rt : response.routeTables()) {
            if (rt.vpcId().equals(vpcId)) {
                for (Tag tag : rt.tags()) {
                    if (tag.key().equals("Name") && tag.value().equals(routeTableName)) {
                        System.out.println("Found existing Route Table: " + routeTableName + " | ID: " + rt.routeTableId());
                        return rt.routeTableId();
                    }
                }
            }
        }
        return null;
    }

    // Check if a Route Table is already associated with a subnet
    private boolean isAssociated(String routeTableId, String subnetId) {
        DescribeRouteTablesResponse response = ec2.describeRouteTables(
                DescribeRouteTablesRequest.builder()
                        .routeTableIds(routeTableId)
                        .build());

        List<RouteTableAssociation> associations = response.routeTables().get(0).associations();
        for (RouteTableAssociation assoc : associations) {
            if (assoc.subnetId() != null && assoc.subnetId().equals(subnetId)) {
                return true;
            }
        }
        return false;
    }

    // Check if Internet route already exists in route table
    private boolean isInternetRoutePresent(String routeTableId, String gatewayId) {
        DescribeRouteTablesResponse response = ec2.describeRouteTables(
                DescribeRouteTablesRequest.builder()
                        .routeTableIds(routeTableId)
                        .build());

        for (Route route : response.routeTables().get(0).routes()) {
            if ("0.0.0.0/0".equals(route.destinationCidrBlock()) &&
                    gatewayId.equals(route.gatewayId()) &&
                    "active".equalsIgnoreCase(route.stateAsString())) {
                return true;
            }
        }
        return false;
    }

    // Method to create a Route Table
    public String createRouteTable(String vpcId, String routeTableName) {
        String existingRtId = findRouteTableByName(vpcId, routeTableName);
        if (existingRtId != null) {
            return existingRtId;
        }

        try {
            CreateRouteTableRequest request = CreateRouteTableRequest.builder()
                    .vpcId(vpcId)
                    .tagSpecifications(TagSpecification.builder()
                            .resourceType(ResourceType.ROUTE_TABLE)
                            .tags(Tag.builder().key("Name").value(routeTableName).build())
                            .build())
                    .build();

            CreateRouteTableResponse response = ec2.createRouteTable(request);
            String routeTableId = response.routeTable().routeTableId();
            System.out.println("Route Table Created: " + routeTableName + " | ID: " + routeTableId);
            return routeTableId;
        } catch (Ec2Exception e) {
            System.err.println("Error creating Route Table: " + e.getMessage());
            return null;
        }
    }

    // Method to associate Route Table with a Subnet
    public void associateRouteTable(String routeTableId, String subnetId) {
        if (isAssociated(routeTableId, subnetId)) {
            System.out.println("Route Table already associated with Subnet: " + subnetId);
            return;
        }

        try {
            ec2.associateRouteTable(AssociateRouteTableRequest.builder()
                    .routeTableId(routeTableId)
                    .subnetId(subnetId)
                    .build());

            System.out.println("Route Table Associated with Subnet: " + subnetId);
        } catch (Ec2Exception e) {
            System.err.println("Error associating Route Table: " + e.getMessage());
        }
    }

    // Method to add a route to the Route Table
    public void addInternetRoute(String routeTableId, String gatewayId) {
        if (isInternetRoutePresent(routeTableId, gatewayId)) {
            System.out.println("Internet route already exists in Route Table: " + routeTableId);
            return;
        }

        try {
            ec2.createRoute(CreateRouteRequest.builder()
                    .routeTableId(routeTableId)
                    .destinationCidrBlock("0.0.0.0/0")
                    .gatewayId(gatewayId)
                    .build());

            System.out.println("Internet Route Added: 0.0.0.0/0 → " + gatewayId);
        } catch (Ec2Exception e) {
            System.err.println("Error adding route: " + e.getMessage());
        }
    }

    /*
    public static void main(String[] args) {
        RouteTableManager manager = new RouteTableManager();

        String vpcId = "vpc-05b4da64c5210d729";
        String subnet1Id = "subnet-02a9938425a83f1d5";
        String subnet2Id = "subnet-0debca590a67c0ad6";
        String gatewayId = "igw-08875f5f7de273e8d";

        String routeTableId = manager.createRouteTable(vpcId, "crimealert-routetable-public");

        if (routeTableId != null) {
            manager.associateRouteTable(routeTableId, subnet1Id);
            manager.associateRouteTable(routeTableId, subnet2Id);
            manager.addInternetRoute(routeTableId, gatewayId);
        }
    }
    */
}
