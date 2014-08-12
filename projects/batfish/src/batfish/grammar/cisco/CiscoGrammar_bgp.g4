parser grammar CiscoGrammar_bgp;

import CiscoGrammarCommonParser;

options {
   tokenVocab = CiscoGrammarCommonLexer;
}

address_family_rb_stanza
:
   ADDRESS_FAMILY
   (
      IPV4
      | IPV6
      | VPNV4
      | VPNV6
   ) MULTICAST?
   (
      VRF vrf_name = VARIABLE
   )? NEWLINE address_family_rb_stanza_tail
   (
      EXIT_ADDRESS_FAMILY NEWLINE
   )? closing_comment?
;

address_family_rb_stanza_tail
:
   (
      afsl += af_stanza
   )+
;

af_stanza
:
   aggregate_address_af_stanza
   | default_metric_af_stanza
   | neighbor_activate_af_stanza
   | neighbor_default_originate_af_stanza
   | neighbor_description_af_stanza
   | neighbor_distribute_list_af_stanza
   | neighbor_ebgp_multihop_af_stanza
   | neighbor_filter_list_af_stanza
   | neighbor_next_hop_self_af_stanza
   | neighbor_peer_group_assignment_af_stanza
   | neighbor_peer_group_creation_af_stanza
   | neighbor_prefix_list_af_stanza
   | neighbor_remote_as_af_stanza
   | neighbor_route_map_af_stanza
   | neighbor_route_reflector_client_af_stanza
   | neighbor_send_community_af_stanza
   | neighbor_shutdown_af_stanza
   | network_af_stanza
   | network6_af_stanza
   | null_af_stanza
   | redistribute_aggregate_af_stanza
   | redistribute_connected_af_stanza
   | redistribute_static_af_stanza
;

aggregate_address_af_stanza
:
   aggregate_address_tail_bgp
;

aggregate_address_rb_stanza
:
   aggregate_address_tail_bgp
;

aggregate_address_tail_bgp
:
   AGGREGATE_ADDRESS
   (
      (
         network = IP_ADDRESS subnet = IP_ADDRESS
      )
      | prefix = IP_PREFIX
      | ipv6_prefix = IPV6_PREFIX
   ) SUMMARY_ONLY? NEWLINE
;

auto_summary_af_stanza
:
   NO? AUTO_SUMMARY NEWLINE
;

cluster_id_bgp_rb_stanza
:
   BGP CLUSTER_ID
   (
      id = DEC
      | id = IP_ADDRESS
   ) NEWLINE
;

default_metric_af_stanza
:
   default_metric_tail_bgp
;

default_metric_rb_stanza
:
   default_metric_tail_bgp
;

default_metric_tail_bgp
:
   DEFAULT_METRIC metric = DEC NEWLINE
;

neighbor_activate_af_stanza
:
   NEIGHBOR
   (
      neighbor = IP_ADDRESS
      | neighbor6 = IPV6_ADDRESS
      | pg = ~( IP_ADDRESS | IPV6_ADDRESS | NEWLINE )
   ) ACTIVATE NEWLINE
;

neighbor_default_originate_af_stanza
:
   neighbor_default_originate_tail_bgp
;

neighbor_default_originate_rb_stanza
:
   neighbor_default_originate_tail_bgp
;

neighbor_default_originate_tail_bgp
:
   NEIGHBOR
   (
      ip = IP_ADDRESS
      | ipv6 = IPV6_ADDRESS
      | peergroup = VARIABLE
   ) DEFAULT_ORIGINATE
   (
      ROUTE_MAP map = VARIABLE
   )? NEWLINE
;

neighbor_description_af_stanza
:
   neighbor_description_tail_bgp
;

neighbor_description_rb_stanza
:
   neighbor_description_tail_bgp
;

neighbor_description_tail_bgp
:
   NEIGHBOR neighbor = ~NEWLINE description_line
;

neighbor_distribute_list_af_stanza
:
   neighbor_distribute_list_tail_bgp
;

neighbor_distribute_list_rb_stanza
:
   neighbor_distribute_list_tail_bgp
;

neighbor_distribute_list_tail_bgp
:
   NEIGHBOR ~NEWLINE DISTRIBUTE_LIST ~NEWLINE* NEWLINE
;

neighbor_ebgp_multihop_af_stanza
:
   neighbor_ebgp_multihop_tail_bgp
;

neighbor_ebgp_multihop_rb_stanza
:
   neighbor_ebgp_multihop_tail_bgp
;

neighbor_ebgp_multihop_tail_bgp
:
   NEIGHBOR neighbor = ~NEWLINE EBGP_MULTIHOP hop = DEC NEWLINE
;

neighbor_filter_list_af_stanza
:
   neighbor_filter_list_tail_bgp
;

neighbor_filter_list_tail_bgp
:
   NEIGHBOR neighbor = . FILTER_LIST num = DEC
   (
      IN
      | OUT
   ) NEWLINE
;

neighbor_next_hop_self_af_stanza
:
   neighbor_next_hop_self_tail_bgp
;

neighbor_next_hop_self_rb_stanza
:
   neighbor_next_hop_self_tail_bgp
;

neighbor_next_hop_self_tail_bgp
:
   NEIGHBOR
   (
      neighbor = IP_ADDRESS
      | neighbor = VARIABLE
   ) NEXT_HOP_SELF NEWLINE
;

neighbor_peer_group_assignment_af_stanza
:
   neighbor_peer_group_assignment_tail_bgp
;

neighbor_peer_group_assignment_rb_stanza
:
   neighbor_peer_group_assignment_tail_bgp
;

neighbor_peer_group_assignment_tail_bgp
:
   NEIGHBOR
   (
      address = IP_ADDRESS
      | address6 = IPV6_ADDRESS
   ) PEER_GROUP name = VARIABLE NEWLINE
;

neighbor_peer_group_creation_af_stanza
:
   neighbor_peer_group_creation_tail_bgp
;

neighbor_peer_group_creation_rb_stanza
:
   neighbor_peer_group_creation_tail_bgp
;

neighbor_peer_group_creation_tail_bgp
:
   NEIGHBOR name = VARIABLE PEER_GROUP NEWLINE
;

neighbor_prefix_list_af_stanza
:
   neighbor_prefix_list_tail_bgp
;

neighbor_prefix_list_rb_stanza
:
   neighbor_prefix_list_tail_bgp
;

neighbor_prefix_list_tail_bgp
:
   NEIGHBOR neighbor = ~NEWLINE PREFIX_LIST list_name = VARIABLE
   (
      IN
      | OUT
   ) NEWLINE
;

neighbor_remote_as_af_stanza
:
   neighbor_remote_as_tail_bgp
;

neighbor_remote_as_rb_stanza
:
   neighbor_remote_as_tail_bgp
;

neighbor_remote_as_tail_bgp
:
   NEIGHBOR pg = ~NEWLINE REMOTE_AS as = DEC NEWLINE
;

neighbor_route_map_af_stanza
:
   neighbor_route_map_tail_bgp
;

neighbor_route_map_rb_stanza
:
   neighbor_route_map_tail_bgp
;

neighbor_route_map_tail_bgp
:
   NEIGHBOR neighbor = ~NEWLINE ROUTE_MAP name = VARIABLE
   (
      IN
      | OUT
   ) NEWLINE
;

neighbor_remove_private_as_af_stanza
:
   NEIGHBOR
   (
      IP_ADDRESS
      | VARIABLE
   ) REMOVE_PRIVATE_AS NEWLINE
;

neighbor_route_reflector_client_af_stanza
:
   NEIGHBOR
   (
      pg = VARIABLE
      | pg = IP_ADDRESS
   ) ROUTE_REFLECTOR_CLIENT NEWLINE
;

neighbor_send_community_af_stanza
:
   neighbor_send_community_tail_bgp
;

neighbor_send_community_rb_stanza
:
   neighbor_send_community_tail_bgp
;

neighbor_send_community_tail_bgp
:
   NEIGHBOR
   (
      neighbor = IP_ADDRESS
      | neighbor = VARIABLE
   ) SEND_COMMUNITY EXTENDED? BOTH? NEWLINE
;

neighbor_shutdown_af_stanza
:
   neighbor_shutdown_tail_bgp
;
   
neighbor_shutdown_rb_stanza
:
   neighbor_shutdown_tail_bgp
;
   
neighbor_shutdown_tail_bgp
:
   NEIGHBOR
   (
      ip = IP_ADDRESS
      | ip6= IPV6_ADDRESS
      | peergroup = VARIABLE
   ) SHUTDOWN NEWLINE
;

neighbor_update_source_rb_stanza
:
   NEIGHBOR neighbor = ~NEWLINE UPDATE_SOURCE source = VARIABLE NEWLINE
;

network_af_stanza
:
   network_tail_bgp
;

network_rb_stanza
:
   network_tail_bgp
;

network_tail_bgp
:
   NETWORK
   (
      (
         ip = IP_ADDRESS
         (
            MASK mask = IP_ADDRESS
         )?
      )
      | prefix = IP_PREFIX
   )? 
   (
      ROUTE_MAP mapname = VARIABLE
   )?
   NEWLINE
;

network6_af_stanza
:
   network6_tail_bgp
;

network6_rb_stanza
:
   network6_tail_bgp
;

network6_tail_bgp
:
   NETWORK
   (
      address = IPV6_ADDRESS
      | prefix = IPV6_PREFIX
   ) NEWLINE
;

no_neighbor_activate_af_stanza
:
   NO NEIGHBOR pg = ~NEWLINE ACTIVATE NEWLINE
;

null_af_stanza
:
   comment_stanza
   | neighbor_remove_private_as_af_stanza
   | no_neighbor_activate_af_stanza
   | null_standalone_af_stanza
;

null_rb_stanza
:
   comment_stanza
   | null_standalone_rb_stanza
;

null_standalone_af_stanza
:
   NO?
   (
      (
         AGGREGATE_ADDRESS IPV6_ADDRESS
      )
      | AUTO_SUMMARY
      | BGP
      | MAXIMUM_PATHS
      |
      (
         NEIGHBOR ~NEWLINE
         (
            MAXIMUM_PREFIX
            | NEXT_HOP_SELF
            | PASSWORD
            | SEND_LABEL
            | SOFT_RECONFIGURATION
            | TIMERS
         )
      )
      | SYNCHRONIZATION
   ) ~NEWLINE* NEWLINE
;

null_standalone_rb_stanza
:
   NO?
   (
      AUTO_SUMMARY
      |
      (
         BGP
         (
            ALWAYS_COMPARE_MED
            | DAMPENING
            | DEFAULT
            | DETERMINISTIC_MED
            | GRACEFUL_RESTART
            | LISTEN
            | LOG_NEIGHBOR_CHANGES
         )
      )
      | MAXIMUM_PATHS
      |
      (
         NEIGHBOR ~NEWLINE
         (
            DESCRIPTION
            | DONT_CAPABILITY_NEGOTIATE
            | FALL_OVER
            | MAXIMUM_PREFIX
            | MAXIMUM_ROUTES
            | PASSWORD
            | REMOVE_PRIVATE_AS
            | SOFT_RECONFIGURATION
            | TIMERS
            | TRANSPORT
            | VERSION
         )
      )
      | SYNCHRONIZATION
   ) ~NEWLINE* NEWLINE
;

rb_stanza
:
   aggregate_address_rb_stanza
   | cluster_id_bgp_rb_stanza
   | default_metric_rb_stanza
   | neighbor_default_originate_rb_stanza
   | neighbor_description_rb_stanza
   | neighbor_distribute_list_rb_stanza
   | neighbor_ebgp_multihop_rb_stanza
   | neighbor_next_hop_self_rb_stanza
   | neighbor_peer_group_creation_rb_stanza
   | neighbor_peer_group_assignment_rb_stanza
   | neighbor_prefix_list_rb_stanza
   | neighbor_remote_as_rb_stanza
   | neighbor_route_map_rb_stanza
   | neighbor_send_community_rb_stanza
   | neighbor_shutdown_rb_stanza
   | neighbor_update_source_rb_stanza
   | network_rb_stanza
   | network6_rb_stanza
   | null_rb_stanza
   | redistribute_aggregate_rb_stanza
   | redistribute_connected_rb_stanza
   | redistribute_ospf_rb_stanza
   | redistribute_static_rb_stanza
   | router_id_bgp_rb_stanza
;

redistribute_aggregate_af_stanza
:
   redistribute_aggregate_tail_bgp
;

redistribute_aggregate_rb_stanza
:
   redistribute_aggregate_tail_bgp
;

redistribute_aggregate_tail_bgp
:
   REDISTRIBUTE AGGREGATE NEWLINE
;

redistribute_connected_af_stanza
:
   redistribute_connected_tail_bgp
;

redistribute_connected_rb_stanza
:
   redistribute_connected_tail_bgp
;

redistribute_connected_tail_bgp
:
   REDISTRIBUTE CONNECTED
   (
      (
         ROUTE_MAP map = VARIABLE
      )
      |
      (
         METRIC metric = DEC
      )
   )* NEWLINE
;

redistribute_ospf_rb_stanza
:
   redistribute_ospf_tail_bgp
;

redistribute_ospf_af_stanza
:
   redistribute_ospf_tail_bgp
;

redistribute_ospf_tail_bgp
:
   REDISTRIBUTE OSPF procnum = DEC
   (
      (
         ROUTE_MAP map = VARIABLE
      )
      |
      (
         METRIC metric = DEC
      )
   )* NEWLINE
;

redistribute_static_af_stanza
:
   redistribute_static_tail_bgp
;

redistribute_static_rb_stanza
:
   redistribute_static_tail_bgp
;

redistribute_static_tail_bgp
:
   REDISTRIBUTE STATIC
   (
      (
         ROUTE_MAP map = VARIABLE
      )
      |
      (
         METRIC metric = DEC
      )
   )* NEWLINE
;

router_bgp_stanza
:
   ROUTER BGP procnum = DEC NEWLINE router_bgp_stanza_tail
;

router_bgp_stanza_tail
:
   (
      rbsl += rb_stanza
   )* closing_comment?
   (
      afrbsl += address_family_rb_stanza
   )*
;

router_id_bgp_rb_stanza
:
   BGP ROUTER_ID routerid = IP_ADDRESS NEWLINE
;
