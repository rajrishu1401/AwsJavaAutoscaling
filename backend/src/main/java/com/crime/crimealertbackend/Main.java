package com.crime.crimealertbackend;

public class Main {
    public static void main(String[] args) {

        /*System.out.println("Crime Alert Application Running...");
        // If needed, you can manually invoke Lambda's handleRequest method
        App app = new App();
        app.handleRequest("Test Input", null);*/
        VpcManager vpcManager = new VpcManager();
        String vpcId=vpcManager.createVpc("12.0.0.0/16", "crimealertvpc");
        String gatewayId;
        String subnet1Id;
        String subnet2Id;
        String routeTableId;
        if(vpcId!=null) {
            InternetGatewayManager igwManager = new InternetGatewayManager();
            gatewayId = igwManager.createInternetGateway("crimealertgateway");
            if (gatewayId != null) {
                igwManager.attachInternetGateway(gatewayId, vpcId);
                SubnetManager subManager = new SubnetManager();
                subnet1Id = subManager.createSubnet(vpcId, "12.0.1.0/24", "ap-south-1a", "crimealert-subnet-1a");
                subnet2Id = subManager.createSubnet(vpcId, "12.0.3.0/24", "ap-south-1b", "crimealert-subnet-1b");
                if (subnet1Id != null && subnet2Id != null) {
                    RouteTableManager manager = new RouteTableManager();
                    routeTableId = manager.createRouteTable(vpcId, "crimealert-routetable-public");
                    if (routeTableId != null) {
                        manager.associateRouteTable(routeTableId, subnet1Id);
                        manager.associateRouteTable(routeTableId, subnet2Id);
                        manager.addInternetRoute(routeTableId, gatewayId);
                        TargetGroupManager tgManager = new TargetGroupManager();
                        String targetGroupArn = tgManager.createTargetGroup(vpcId, "crimealert-tg-ec2");
                        if (targetGroupArn != null) {
                            String securityGroupId = SecurityGroupManager.createSecurityGroup(vpcId);
                            System.out.println("Security Group ID: " + securityGroupId);
                            if(securityGroupId!=null){
                                String lbArn = LoadBalancerManager.createLoadBalancer(vpcId, subnet1Id, subnet2Id, securityGroupId);
                                if (lbArn != null) {
                                    System.out.println("Waiting for Load Balancer to become active...");
                                    while (!LoadBalancerManager.isLoadBalancerActive(lbArn)) {
                                        System.out.println("Load Balancer is still provisioning...");
                                        try {
                                            Thread.sleep(10000);  // Check every 10 seconds
                                        } catch (InterruptedException e) {
                                            System.err.println("Load Balancer provisioning wait interrupted.");
                                        }
                                    }
                                    System.out.println("Load Balancer is now ACTIVE!");
                                    LoadBalancerManager.createListener(lbArn, targetGroupArn);
                                    String securityGroupForTemId = SecurityGroupManager.createSecurityGroupForTemplate(vpcId,securityGroupId);
                                    System.out.println("Security Group ID: " + securityGroupForTemId);
                                    if(securityGroupForTemId!=null){
                                        String keyPairName=KeyPairManager.createKeyPair("crimealert-key");
                                        if(keyPairName!=null){
                                            String ltId=LaunchTemplateManager.createLaunchTemplate(securityGroupForTemId, "crimealert-key");
                                            if(ltId!=null){
                                                String vpcZoneIdentifier = subnet1Id+","+subnet2Id;
                                                AutoScalingGroupManager.createAutoScalingGroup(vpcZoneIdentifier, targetGroupArn);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
