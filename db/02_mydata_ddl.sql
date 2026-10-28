-- =====================================================================
-- myData 시스템 DDL (PostgreSQL) - 별도 DB
-- RIA 관리자는 이 테이블을 직접 SELECT 하지 않고 API(ci_hash 기준)로만 조회
--
-- 실행 순서:
--   1) 관리 DB(postgres)에 접속해서:  CREATE DATABASE mydata;
--   2) mydata DB로 접속 후 아래 실행:  \c mydata   (psql)  또는 DBeaver에서 mydata 선택
-- =====================================================================

DROP TABLE IF EXISTS mydata_trade CASCADE;
DROP TABLE IF EXISTS mydata_fund CASCADE;
DROP TABLE IF EXISTS mydata_ria_account CASCADE;
DROP TABLE IF EXISTS mydata_key CASCADE;

-- 전 인구(10만) 중앙 키 테이블
CREATE TABLE mydata_key (
                            ci_hash VARCHAR(64) NOT NULL,
                            PRIMARY KEY (ci_hash)
);
COMMENT ON TABLE  mydata_key         IS 'myData 전 인구 키';
COMMENT ON COLUMN mydata_key.ci_hash IS 'HMAC-SHA256(정규화 주민번호, 공유 PEPPER). RIA/증권사와 동일 값';

-- 타사 RIA 계좌(A4 설정한도 합산 대상)
CREATE TABLE mydata_ria_account (
                                    mydata_account_id   BIGINT        GENERATED ALWAYS AS IDENTITY,
                                    ci_hash             VARCHAR(64)   NOT NULL,
                                    broker_name         VARCHAR(50)   NOT NULL,
                                    ria_limit           DECIMAL(15,2) NOT NULL,
                                    ria_cumulative_sell DECIMAL(15,2) NOT NULL,
                                    PRIMARY KEY (mydata_account_id),
                                    CONSTRAINT fk_mydata_ria__key FOREIGN KEY (ci_hash) REFERENCES mydata_key (ci_hash),
                                    CONSTRAINT uk_mydata_ria_account_ci_broker UNIQUE (ci_hash, broker_name)
);
COMMENT ON TABLE  mydata_ria_account                     IS '타사 RIA 계좌';
COMMENT ON COLUMN mydata_ria_account.broker_name         IS '증권사명';
COMMENT ON COLUMN mydata_ria_account.ria_limit           IS '해당 증권사 RIA 설정한도(A4 합산 대상)';
COMMENT ON COLUMN mydata_ria_account.ria_cumulative_sell IS '해당 증권사 RIA 누적매도금액(크로스체크 보조)';

-- 국내 설정 펀드 마스터(G2 대상상품 판별용)
CREATE TABLE mydata_fund (
                             mydata_fund_id       BIGINT        GENERATED ALWAYS AS IDENTITY,
                             fund_code            VARCHAR(12)   NOT NULL,
                             fund_name            VARCHAR(100)  NOT NULL,
                             foreign_stock_ratio  DECIMAL(5,2)  NULL,
                             inception_date       DATE          NULL,
                             PRIMARY KEY (mydata_fund_id),
                             CONSTRAINT uq_mydata_fund__fund_code UNIQUE (fund_code)
);
COMMENT ON TABLE  mydata_fund                     IS '국내 설정 펀드 마스터(G2 대상상품 판별용)';
COMMENT ON COLUMN mydata_fund.fund_code           IS '펀드표준코드(KSD, 12자리)';
COMMENT ON COLUMN mydata_fund.foreign_stock_ratio IS '해외주식 비중%. G2 60% 요건 판정';
COMMENT ON COLUMN mydata_fund.inception_date      IS '설정일. G2 1개월 경과 판정';

-- 전 금융기관 매매(연동 감시 원천, G1/G3, G2)
CREATE TABLE mydata_trade (
                              trade_id    BIGINT        GENERATED ALWAYS AS IDENTITY,
                              ci_hash     VARCHAR(64)   NOT NULL,
                              broker_name VARCHAR(50)   NOT NULL,
                              trade_type  VARCHAR(15)   NOT NULL,
                              stock_type  VARCHAR(15)   NOT NULL,
                              fund_code   VARCHAR(12)   NULL,
                              ticker      VARCHAR(20)   NULL,
                              qty         DECIMAL(15,2) NOT NULL,
                              trade_date  DATE          NOT NULL,
                              amount      DECIMAL(15,2) NOT NULL,
                              PRIMARY KEY (trade_id),
                              CONSTRAINT chk_mydata_trade_type       CHECK (trade_type IN ('BUY','SELL','INHERITANCE','GIFT')),
                              CONSTRAINT chk_mydata_trade_stock_type CHECK (stock_type IN ('FOREIGN_STOCK','ETF','ETN','FUND')),
                              CONSTRAINT fk_mydata_trade__key        FOREIGN KEY (ci_hash) REFERENCES mydata_key (ci_hash),
                              CONSTRAINT fk_mydata_trade__fund       FOREIGN KEY (fund_code) REFERENCES mydata_fund (fund_code),
                              CONSTRAINT chk_mydata_trade_fund_code  CHECK ((stock_type = 'FUND' AND fund_code IS NOT NULL) OR (stock_type <> 'FUND' AND fund_code IS NULL)),
                              CONSTRAINT chk_mydata_trade_ticker     CHECK ((stock_type <> 'FUND' AND ticker IS NOT NULL) OR (stock_type = 'FUND' AND ticker IS NULL))
);
COMMENT ON TABLE  mydata_trade             IS '전 금융기관 매매';
COMMENT ON COLUMN mydata_trade.broker_name IS '증권사/금융기관';
COMMENT ON COLUMN mydata_trade.trade_type  IS 'BUY/SELL/INHERITANCE/GIFT';
COMMENT ON COLUMN mydata_trade.stock_type  IS 'FOREIGN_STOCK/ETF/ETN/FUND';
COMMENT ON COLUMN mydata_trade.fund_code   IS '펀드표준코드(stock_type=FUND인 경우만, mydata_fund 참조)';
COMMENT ON COLUMN mydata_trade.ticker      IS '종목 티커(stock_type이 FOREIGN_STOCK/ETF/ETN인 경우만)';
COMMENT ON COLUMN mydata_trade.qty         IS '수량';
COMMENT ON COLUMN mydata_trade.trade_date  IS '결제일 기준';
COMMENT ON COLUMN mydata_trade.amount      IS '금액';