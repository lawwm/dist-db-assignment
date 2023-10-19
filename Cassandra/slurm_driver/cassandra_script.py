#!/usr/bin/python
import yaml
import sys
import socket

# Get the current IP Address of the node
def get_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.settimeout(0)
    try:
        # doesn't even have to be reachable
        s.connect(('10.254.254.254', 1))
        IP = s.getsockname()[0]
    except Exception:
        IP = '127.0.0.1'
    finally:
        s.close()
    return IP

def generate_yaml(input, output, seeds):
    with open(input, 'r') as istream:
        ip_addr = get_ip()
        ymldoc = yaml.safe_load(istream)
        ymldoc['listen_address'] = ip_addr
        ymldoc['rpc_address'] = '0.0.0.0'
        ymldoc['broadcast_rpc_address'] = ip_addr
        ymldoc['seed_provider'][0]['parameters'][0]['seeds'] = seeds
        ymldoc['materialized_views_enabled'] = True

        with open(output, "w") as ostream:
            yaml.dump(ymldoc, ostream, default_flow_style=False, sort_keys=False)
        
        
def main():
    input = sys.argv[1]
    output = sys.argv[2]
    seeds = sys.argv[3]
    generate_yaml(input, output, seeds)    
            
if __name__ == "__main__":
    main()