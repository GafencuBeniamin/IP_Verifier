#include <jni.h>
#include <string>
#include <ifaddrs.h>
#include <arpa/inet.h>
#include <net/if.h>
#include <netdb.h>

// Function to check if an IPv6 address is in the global unicast range
bool isGlobalUnicastIPv6(const std::string& ip) {
    struct in6_addr addr;
    if (inet_pton(AF_INET6, ip.c_str(), &addr) != 1) {
        return false;
    }
    // Check if the first byte of the address is in the range for global unicast
    return (addr.s6_addr[0] & 0xE0) == 0x20;
}

// Function to check if an IPv4 address is public
bool isPublicIPv4(const std::string& ip) {
    struct in_addr addr;
    if (inet_pton(AF_INET, ip.c_str(), &addr) != 1) {
        return false;
    }

    uint32_t h = ntohl(addr.s_addr); //converting from network to host byte order
    return !((h >> 24 == 10) ||                                 // 10.0.0.0/8
             (h >> 20 == 0xAC1) ||                              // 172.16.0.0/12
             (h >> 16 == 0xC0A8) ||                             // 192.168.0.0/16
             (h >> 24 == 0x7F) ||                               // 127.0.0.0/8 (loopback)
             (h >= 0xA9FE0000 && h <= 0xA9FEFFFF));             // 169.254.0.0/16 (link-local)
}
// Function to check if an IPv4 is not loopback
bool isNotLoopbackIPv4(const std::string& ip) {
    struct in_addr addr;
    if (inet_pton(AF_INET, ip.c_str(), &addr) != 1) {
        return false;
    }

    uint32_t h = ntohl(addr.s_addr);
    return !(h >> 24 == 0x7F);// 127.0.0.0/8 (loopback)
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_app_cloudonixtest_MainActivity_getIPAddress(JNIEnv* env, jobject /* this */) {
    struct ifaddrs *ifaddr, *ifa;
    char addr[NI_MAXHOST];
    std::string ip;

    if (getifaddrs(&ifaddr) == -1) {
        return env->NewStringUTF("Error retrieving IP");
    }

    std::string globalUnicastIPv6;
    std::string publicIPv4;
    std::string anyIPv4;

    for (ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next) {
        if (ifa->ifa_addr == NULL) continue;

        int family = ifa->ifa_addr->sa_family;
        if (family == AF_INET || family == AF_INET6) {
            int s = getnameinfo(ifa->ifa_addr,
                                (family == AF_INET) ? sizeof(struct sockaddr_in) : sizeof(struct sockaddr_in6),
                                addr, NI_MAXHOST, NULL, 0, NI_NUMERICHOST);
            if (s != 0) continue;

            std::string ipStr = addr;
            if (family == AF_INET6 && isGlobalUnicastIPv6(ipStr)) {
                globalUnicastIPv6 = ipStr;
                break;
            } else if (family == AF_INET && isPublicIPv4(ipStr)) {
                publicIPv4 = ipStr;
            } else if (family == AF_INET && isNotLoopbackIPv4(ipStr)) {
                anyIPv4 = ipStr;
            }
        }
    }

    freeifaddrs(ifaddr);

    if (!globalUnicastIPv6.empty()) {
        ip = globalUnicastIPv6;
    } else if (!publicIPv4.empty()) {
        ip = publicIPv4;
    } else if (!anyIPv4.empty()) {
        ip = anyIPv4;
    } else {
        ip = "No valid IP found";
    }

    return env->NewStringUTF(ip.c_str());
}
