# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Electronic Guarantee System (电子保函系统) - A B2B platform connecting:
- **Upstream**: Trading systems (public resource transaction centers, bidding platforms)
- **Downstream**: Financial institutions (banks, insurance companies, guarantee companies)

## Documentation Structure

The project currently contains requirement documents in `docs/`:

```
docs/
├── 01_上游交易系统对接/      # Upstream interface documentation
├── 02_电子保函平台说明/      # Core business documentation
├── 03_下游出函机构/          # Downstream interface documentation
├── 04_其它/                  # Other documents
├── 05_电子保函需求分析/       # Original requirement analysis
└── 05_电子保函需求分析_v2/   # Latest requirement analysis (v2)
```

Key documentation:
- `docs/05_电子保函需求分析_v2/README.md` - Main requirement index
- `AGENTS.md` - Project guidelines and technical overview

## Business Domain

### Business Types
- Bank electronic guarantees
- Separate bank guarantees (guarantee company + bank)
- Insurance guarantee policies
- Guarantee guarantees

### Use Cases
- Bid guarantee (≤80万)
- Performance guarantee (5-10% of contract value)
- Quality guarantee (3% of contract value)
- Migrant worker wage guarantee

### Core Flow
```
Project Info Sync → Application → Payment → Guarantee Issuance → E-Signature → Verification
                                    ↓
                            Cancellation/Claims
```

## Key Technical Points

### 1. Interface Security (Critical)

**Signature Algorithms**:
| Algorithm | Key | Platforms |
|-----------|-----|----------|
| SM3 | appkey + appsecret | 数字泸州, 眉山 |
| MD5 | appkey + appsecret | 蔓延 |
| SM2 | CA certificate | Some standards |

**Encryption**:
| Scheme | Key Source | Platform |
|--------|------------|----------|
| Digital envelope (SM4 + SM2) | Bidder CA certificate | 数字泸州 |
| Platform key (SM4) | Platform appSecret | 江西省 |

### 2. Technology Stack

| Component | Selection |
|-----------|-----------|
| Framework | Pig (Spring Cloud Alibaba) |
| Workflow | Flowable |
| Distributed Transaction | Seata AT |
| Gateway | Spring Cloud Gateway |
| Registry/Config | Nacos |
| Database | MySQL |
| Cache | Redis |

## Platform Configuration

Each upstream platform requires independent configuration:
- `platformCode` - Platform code
- `signAlgorithm` - Signature algorithm (SM3/MD5/SM2)
- `encryptAlgorithm` - Encryption algorithm
- `signParams` - Parameters for signing
- `secretKey` - appSecret
- `caCertPath` - CA certificate path (digital envelope mode)

## Common Tasks

- Read requirement documents in `docs/05_电子保函需求分析_v2/`
- Check upstream interface specifications in `docs/01_上游交易系统对接/`
- Review downstream interface specs in `docs/03_下游出函机构/`
- Reference AGENTS.md for technical overview
