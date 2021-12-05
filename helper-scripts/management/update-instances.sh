#!/bin/bash
export IFS="
"
for  instances in $(cf curl v3/service_offerings?names=osb-postgresql-test | jq -r '[.resources[].guid]|join(",")' | xargs -I{} cf curl /v3/service_plans?service_offering_guids={} | jq -r '[.resources[].guid]|join(",")' | xargs -I{} cf curl v3/service_instances?service_plan_guids={} | jq -c '.resources[]'); do
guid="$(jq -r '.guid' <<<"$instances")"
  if [ "$( cf curl v3/service_instances/ae74bc06-9a62-449e-b1a8-28461b294e6a | jq -r '.["last_operation"].state')" == "succeeded" ]; then
    parameters="$(cf curl v3/service_instances/$guid/parameters | jq '.postgres')"
    if [ "$parameters" != "null" ]; then
      echo cf curl -X PATCH v3/service_instances/$guid -d "'""$(jq -c --argjson parameters "$parameters"  '{"name": .name, "tags": .tags, "relationships": { "service_plan": .relationships["service_plan"]}, "parameters":{ "postgres": $parameters }, "metadata":{ "labels": .metadata.labels, "annotations": .metadata.annotations}}' <<<"$instances")""'"
    else
      echo '#………'"$( jq -r '.name' <<<"$instances" )" has no parameter
    fi
  else
    echo '#………'"$( jq -r '.name' <<<"$instances" )" not in state success
  fi
done

